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

import com.sixsq.slipstream.exceptions.CannotAdvanceFromTerminalStateException;
import com.sixsq.slipstream.exceptions.InvalidStateException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
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

	private State globalState;
	private Map<String, State> nodeStates = new HashMap<String, State>();

	public StateMachine(Map<String, State> nodeStates, State globalState) {
		this.globalState = globalState;
		this.nodeStates = nodeStates;
	}

	public static ExtrinsicState createNodeExtrinsicState(Run run, String node) {
		String key;
		String nodePrefix = node + RuntimeParameter.NODE_PROPERTY_SEPARATOR;
		key = nodePrefix + RuntimeParameter.COMPLETE_KEY;
		RuntimeParameter completed = run.getRuntimeParameters().get(key);

		key = nodePrefix + RuntimeParameter.ABORT_KEY;
		RuntimeParameter failing = run.getRuntimeParameters().get(key);

		RuntimeParameter state = run.getRuntimeParameters().get(
				nodePrefix + RuntimeParameter.STATE_KEY);

		ExtrinsicState extrinsicState = new ExtrinsicState(completed, failing,
				state);
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

	public void start() throws SlipStreamException {
		EntityManager em = beginTransation();
		completeAllNodesState();
		commitTransaction(em);
	}

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

		return nodeStates.get(nodeName).getState();
	}

	private void completeCurrentState(String nodeName)
			throws InvalidStateException, SlipStreamClientException {
		EntityManager em = beginTransation();
		setNodeStateCompleted(nodeName);
		commitTransaction(em);
	}

	private void tryAdvanceState() throws InvalidStateException,
			CannotAdvanceFromTerminalStateException {
		EntityManager em = null;
		try {
			em = beginTransation();
			attemptToAdvanceState();
			commitTransaction(em);
		} catch (RuntimeException ex) {
            if (em != null) {
                if (em.getTransaction() != null && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                em.close();
            }
			logger.warning("Error in tryAdvanceState. Retrying...");
			tryAdvanceState();
		}
	}

	private void recreateStateFromRun(Run run) throws InvalidStateException {
		String nodeNames = run.getNodeNames();
		String[] nodes = nodeNames.split(", ");

		nodeStates = new HashMap<String, State>();
		for (String node : nodes) {
			ExtrinsicState extrinsicState = createNodeExtrinsicState(run, node);
			State nodeState = StateFactory.createInstance(extrinsicState);

			nodeStates.put(node, nodeState);
		}

		ExtrinsicState globalExtrinsicState = createGlobalExtrinsicState(run);
		globalState = StateFactory.createInstance(globalExtrinsicState);
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

		if (globalState.isFinal()) {
			throw (new CannotAdvanceFromTerminalStateException());
		}

		try {
			if (checkSynchronizedConditionMet()) {
				setState(globalState.nextState);
				// Remark LS: I think that the 2 lines below are already executed in the above call
				alineAllNodesStateToGlobalState();
				resetNodesStateCompleted();
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
		if (!force) {
			checkSynchronizedConditionMet();
		}
		globalState = assignNodeState(globalState, newState);
		globalState.getExtrinsicState().setState(newState);
		alineAllNodesStateToGlobalState();
		resetNodesStateCompleted();

	}

	private State assignNodeState(State state, States newState)
			throws InvalidStateException {
		return StateFactory.createInstance(newState, state.getExtrinsicState());
	}

	private boolean checkSynchronizedConditionMet()
			throws InvalidStateException {

		if (checkSynchronizeOnFailure()) {
			return true;
		}

		if (checkSynchronizeNormalCondition()) {
			return true;
		}

		for (String nodeName : nodeStates.keySet()) {
			checkStateCompleted(nodeName);
		}
		return true;
	}

	private boolean checkSynchronizeNormalCondition() {
		boolean isFailing = globalState.isFailing();
		boolean mustSynchronize = globalState.mustSynchronizeNormalCondition();
		return !isFailing && !mustSynchronize;
	}

	private boolean checkSynchronizeOnFailure() {
		boolean isFailing = globalState.isFailing();
		boolean mustSynchronize = globalState.mustSynchronizeOnFailure();
		return isFailing && !mustSynchronize;
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

	public States getNodeState(String nodeName) {
		return nodeStates.get(nodeName).getState();
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
