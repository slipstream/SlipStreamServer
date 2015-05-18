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

import com.sixsq.slipstream.exceptions.*;
import com.sixsq.slipstream.util.Logger;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

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

	private static final int MAX_RECURSION = 100;

	private Run run;
	private State globalState;
	private Map<String, State> nodeStates = new HashMap<String, State>();

	public StateMachine(Run run) throws InvalidStateException {
		recreateStateFromRun(run);
	}

	public static ExtrinsicState createNodeExtrinsicState(Run run, String node) {
		String key;
		String nodePrefix = node + RuntimeParameter.NODE_PROPERTY_SEPARATOR;
		key = nodePrefix + RuntimeParameter.COMPLETE_KEY;
		RuntimeParameter completed = run.getRuntimeParameter(key);

		key = nodePrefix + RuntimeParameter.ABORT_KEY;
		RuntimeParameter failing = run.getRuntimeParameter(key);

		key = nodePrefix + RuntimeParameter.IS_ORCHESTRATOR_KEY;
		RuntimeParameter isOrchestrator = run.getRuntimeParameter(key);

		key = nodePrefix + RuntimeParameter.SCALE_STATE_KEY;
		RuntimeParameter scaleState = run.getRuntimeParameter(key);

		RuntimeParameter state = run.getRuntimeParameter(RuntimeParameter.GLOBAL_STATE_KEY);

		ExtrinsicState extrinsicState = new ExtrinsicState(completed, failing, isOrchestrator, state, scaleState);
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
				globalCompleteRuntimeParameter,
				globalFailingRuntimeParameter,
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

		// nodeInstances and orchestrators are not persisted and only
		// available in the json part of the run
		// Since the StateMachine requires a managed run (i.e. attached to
		// an EntityManager), it cannot be deserialized from json since it
		// would detach the run object from the entity manager.  We therefore
		// need to extract the node instances and orchestrators from a locally
		// deserialized run, but attach the managed run to the extrinsic
		// state of the state machine.
		Run deserializedRun = null;
		try {
			deserializedRun = Run.fromJson(run.getJson());
		} catch (SlipStreamClientException e) {
			throw new SlipStreamClientRuntimeException(e);
		}

		nodeStates = new HashMap<String, State>();
		for (String node : deserializedRun.getNodeInstances()) {
			ExtrinsicState extrinsicState = createNodeExtrinsicState(run, node);
			State nodeState = StateFactory.createInstance(extrinsicState);

			nodeStates.put(node, nodeState);
		}
		for (String orchestrator : deserializedRun.getOrchestrators()) {
			ExtrinsicState extrinsicState = createNodeExtrinsicState(run, orchestrator);
			State nodeState = StateFactory.createInstance(extrinsicState);

			nodeStates.put(orchestrator, nodeState);
		}

		ExtrinsicState globalExtrinsicState = createGlobalExtrinsicState(run);
		globalState = StateFactory.createInstance(globalExtrinsicState);
	}

	public States updateState()
			throws SlipStreamClientException, InvalidStateException {

		tryAdvanceState();

		return globalState.getState();
	}

	void completeCurrentState(String nodeName)
			throws InvalidStateException, SlipStreamClientException {
		setNodeStateCompleted(nodeName);
	}

	private void tryAdvanceState()
			throws InvalidStateException, CannotAdvanceFromTerminalStateException {
		tryAdvanceState(false);
	}

	public void tryAdvanceState(boolean force)
			throws InvalidStateException, CannotAdvanceFromTerminalStateException {
		tryAdvanceToState(globalState.nextState, force);
	}

	public void tryAdvanceToProvisionning() throws InvalidStateException, CannotAdvanceFromTerminalStateException {
		if (!States.Ready.equals(globalState.getState())) {
			throw new InvalidStateException("Transition from " + globalState + " to Provisioning not allowed.");
		} else if (!run.isMutable()) {
			throw new InvalidStateException(
					"Transition from " + globalState + " to Provisioning not allowed in an imutable Run");
		}
		attemptToAdvanceToState(States.Provisioning, true);
	}

	public void tryAdvanceToFinalizing() throws InvalidStateException, CannotAdvanceFromTerminalStateException {
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

	public void tryAdvanceToCancelled() throws InvalidStateException, CannotAdvanceFromTerminalStateException {
		if (!canCancel()){
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
		try {
			attemptToAdvanceToState(state, force);
		} catch (RuntimeException ex) {
			if (recursions > 0) {
				Logger.warning("Error in tryAdvanceToState " + state + ". Retrying...");
				tryAdvanceToState(state, force, recursions - 1);
			} else {
				throw ex;
			}
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

	void setState(States newState, boolean force) throws InvalidStateException {

		globalState = assignNodeState(globalState, newState);

		run.setState(globalState.getState());
		run.setLastStateChange();

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
				if (!node.getValue().isRemoved() &&
					((!onlyOrch && !recoveryMode) ||
					((onlyOrch || recoveryMode) && node.getValue().isOrchestrator()))
				) {
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
		nodeStates.get(nodeName).setFailing(true);
		globalState.setFailing(true);
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

	public boolean isFailing() {
		return globalState.isFailing();
	}

}
