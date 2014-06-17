package com.sixsq.slipstream.statemachine;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.exceptions.CannotAdvanceFromTerminalStateException;
import com.sixsq.slipstream.exceptions.InvalidStateException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;

/**
 * Unit tests:
 * @see StateMachinetTest
 * @see StateMachineMultiThreadingTest
 *
 */
public class StateMachine {

	private static Logger logger = Logger.getLogger("com.sixsq.slipstream.statemachine");
	private static final int MAX_RECURSION = 100;

	private Run run;
	private State globalState;
	private Map<String, State> nodeStates = new HashMap<String, State>();

	public StateMachine(Map<String, State> nodeStates, State globalState, Run run) {
		this.globalState = globalState;
		this.nodeStates = nodeStates;
		this.run = run;
	}

	public StateMachine(Run run) throws InvalidStateException {
		recreateStateFromRun(run);
	}

	public static ExtrinsicState createNodeExtrinsicState(Run run, String node) {
		String key;
		String nodePrefix = node + RuntimeParameter.NODE_PROPERTY_SEPARATOR;
		key = nodePrefix + RuntimeParameter.COMPLETE_KEY;
		RuntimeParameter completed = run.getRuntimeParameters().get(key);

		key = nodePrefix + RuntimeParameter.ABORT_KEY;
		RuntimeParameter failing = run.getRuntimeParameters().get(key);

		key = nodePrefix + RuntimeParameter.IS_ORCHESTRATOR_KEY;
		RuntimeParameter isOrchestrator = run.getRuntimeParameters().get(key);

		RuntimeParameter state = run.getRuntimeParameters().get(RuntimeParameter.GLOBAL_STATE_KEY);

		ExtrinsicState extrinsicState = new ExtrinsicState(completed, failing,
				isOrchestrator, state);
		return extrinsicState;
	}

	public static ExtrinsicState createGlobalExtrinsicState(Run run) {
		RuntimeParameter globalCompleteRuntimeParameter = run
				.getRuntimeParameters().get(RuntimeParameter.GLOBAL_COMPLETE_KEY);
		RuntimeParameter globalFailingRuntimeParameter = run
				.getRuntimeParameters().get(RuntimeParameter.GLOBAL_ABORT_KEY);
		RuntimeParameter globalStateRuntimeParameter = run
				.getRuntimeParameters().get(RuntimeParameter.GLOBAL_STATE_KEY);

		ExtrinsicState globalExtrinsicState = new ExtrinsicState(
				globalCompleteRuntimeParameter, globalFailingRuntimeParameter,
				globalStateRuntimeParameter);
		return globalExtrinsicState;
	}

	public static StateMachine createStateMachine(Run run) {
		StateMachine sc;
		try {
			sc = getStateMachine(run);
		} catch (InvalidStateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		} catch (SlipStreamClientException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		}
		return sc;
	}

	public static StateMachine getStateMachine(Run run) throws ValidationException,
			InvalidStateException, NotFoundException {

		return new StateMachine(run);
	}

	private void recreateStateFromRun(Run run) throws InvalidStateException {
		this.run = run;

		nodeStates = new HashMap<String, State>();
		for (String node : run.getNodeNameList()) {
			ExtrinsicState extrinsicState = createNodeExtrinsicState(run, node);
			State nodeState = StateFactory.createInstance(extrinsicState);

			nodeStates.put(node, nodeState);
		}

		ExtrinsicState globalExtrinsicState = createGlobalExtrinsicState(run);
		globalState = StateFactory.createInstance(globalExtrinsicState);
	}

	// Note: Used only for unit tests
	public void start() throws SlipStreamException {
		EntityManager em = beginTransation();
		completeAllNodesState();
		commitTransaction(em);
	}

	// Note: Used only for unit tests
	private void completeAllNodesState() throws SlipStreamClientException {
		for (String nodeName : nodeStates.keySet()) {
			setNodeStateCompleted(nodeName);
		}
		attemptToAdvanceState();
	}

	public States updateState(String nodeName)
			throws SlipStreamClientException, InvalidStateException {

		completeCurrentState(nodeName);

		tryAdvanceState();

		return globalState.getState();
	}

	private void completeCurrentState(String nodeName)
			throws InvalidStateException, SlipStreamClientException {
		EntityManager em = beginTransation();
		setNodeStateCompleted(nodeName);
		commitTransaction(em);
	}

	private void tryAdvanceState()
			throws InvalidStateException, CannotAdvanceFromTerminalStateException {
		tryAdvanceState(false);
	}

	public void tryAdvanceState(boolean force)
			throws InvalidStateException, CannotAdvanceFromTerminalStateException {
		tryAdvanceToState(globalState.nextState, force);
	}

	public void tryAdvanceToFinalizing() throws InvalidStateException,
			CannotAdvanceFromTerminalStateException {
		if (canCancel()){
			throw new InvalidStateException("Transition from " + globalState + " to Finalizing not allowed.");
		}
		tryAdvanceToState(States.Finalizing, true);
	}

	public void tryAdvanceToDone() throws InvalidStateException,
			CannotAdvanceFromTerminalStateException {
		if (canCancel()){
			throw new InvalidStateException("Transition from " + globalState + " to Done not allowed.");
		}
		tryAdvanceToState(States.Done, true);
	}

	public void tryAdvanceToCancelled() throws InvalidStateException,
	CannotAdvanceFromTerminalStateException {
		if (! canCancel()){
			throw new InvalidStateException("Transition from " + globalState + " to Cancelled not allowed.");
		}
		tryAdvanceToState(States.Cancelled, true);
	}

	public boolean canCancel() {

		return ! States.canTerminate().contains(globalState.getState());
	}

	private void tryAdvanceToState(States state, boolean force)
			throws InvalidStateException, CannotAdvanceFromTerminalStateException {
		tryAdvanceToState(state, force, MAX_RECURSION);
	}

	private void tryAdvanceToState(States state, boolean force, int recursions)
			throws InvalidStateException, CannotAdvanceFromTerminalStateException {
		EntityManager em = null;
		try {
			em = beginTransation();
			attemptToAdvanceToState(state, force);
			commitTransaction(em);
		} catch (RuntimeException ex) {
            if (em != null) {
                if (em.getTransaction() != null && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                em.close();
            }

			if (recursions > 0) {
				logger.warning("Error in tryAdvanceToState " + state + ". Retrying...");
				tryAdvanceToState(state, force, recursions - 1);
			} else {
				throw ex;
			}
		}
		if (em.isOpen()) {
			em.close();
		}
	}

	private Run getRun() {
		return globalState.getExtrinsicState().getRun();
	}

	private void setNodeStateCompleted(String nodeName)
			throws SlipStreamClientException {
		try {
			nodeStates.get(nodeName).setStateCompleted(true);
		} catch (NullPointerException ex) {
			throw (new SlipStreamClientException("Failed to find nodename: "
					+ nodeName));
		}
	}

	private void attemptToAdvanceState()
			throws CannotAdvanceFromTerminalStateException {
		attemptToAdvanceToState(globalState.nextState, false);
	}

	private void attemptToAdvanceToState(States nextState, boolean force)
			throws CannotAdvanceFromTerminalStateException {

		if (globalState.isFinal()) {
			throw (new CannotAdvanceFromTerminalStateException());
		}

		try {
			if (force || checkSynchronizedConditionMet()) {
				setState(nextState, force);
			}
		} catch (InvalidStateException e) {
			return;
		}
	}

	private void setState(States newState) throws InvalidStateException {
		setState(newState, false);
	}

	protected void setState(States newState, boolean force)
			throws InvalidStateException {

		globalState = assignNodeState(globalState, newState);

		run.setState(globalState.getState());

		resetNodesStateCompleted();

		if (globalState.isFinal()) {
			run.setEnd();
		}
	}

	private State assignNodeState(State state, States newState)
			throws InvalidStateException {
		return StateFactory.createInstance(newState, state.getExtrinsicState());
	}

	private boolean checkSynchronizedConditionMet()
			throws InvalidStateException {

		boolean onlyOrch = globalState.synchronizedForOrchestrators();
		boolean recoveryMode = Run.isInRecoveryMode(run);

		if (globalState.synchronizedForEveryone() || onlyOrch) {
			for (Map.Entry<String, State> node : nodeStates.entrySet()) {
				if ((!onlyOrch && !recoveryMode) ||
					((onlyOrch || recoveryMode) && node.getValue().isOrchestrator())) {
					checkStateCompleted(node.getKey());
				}
			}
		}

		return true;
	}

	private void checkStateCompleted(String nodeName)
			throws InvalidStateException {
		State state = nodeStates.get(nodeName);
		if (!state.isStateCompleted()) {
			throw (new InvalidStateException(
					"Synchronization condition not met for node: " + nodeName));
		}
	}

	public void failCurrentState(String nodeName) throws InvalidStateException {
		EntityManager em = beginTransation();
		nodeStates.get(nodeName).setFailing(true);
		globalState.setFailing(true);
		commitTransaction(em);
	}

	protected EntityManager beginTransation() throws InvalidStateException {
		EntityManager em = PersistenceUtil.createEntityManager();
		em.getTransaction().begin();
		Run run = em.find(Run.class, getRun().getResourceUri());
		recreateStateFromRun(run);
		return em;
	}

	protected void commitTransaction(EntityManager em) {
		em.getTransaction().commit();
		em.close();
	}

	private void alineAllNodesStateToGlobalState() throws InvalidStateException {
		for (String nodeName : nodeStates.keySet()) {
			nodeStates.put(
					nodeName,
					assignNodeState(nodeStates.get(nodeName),
							globalState.getState()));
		}
	}

	private void resetNodesStateCompleted() {
		for (State nodeState : nodeStates.values()) {
			nodeState.setStateCompleted(false);
		}
	}

	public States getState() {
		return globalState.getState();
	}

	public void fail(String nodeName) throws InvalidStateException {
		EntityManager em = beginTransation();
		State state = nodeStates.get(nodeName);
		state.setFailing(true);
		globalState.setFailing(true);
		setState(States.SendingReports);
		alineAllNodesStateToGlobalState();
		commitTransaction(em);
	}

	public boolean isFailing() {
		return globalState.isFailing();
	}

}
