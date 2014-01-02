package com.sixsq.slipstream.run;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.statemachine.States;

public class RuntimeParameterStateMachineTest extends
		RuntimeParameterResourceTestBase {

	private static String orchestratorStateKey;
	private static String node1StateKey;
	private static String node2StateKey;
	private static String node1CompleteKey;
	private static String node2CompleteKey;

	@Before
	public void setupClass() throws ValidationException, NotFoundException {
		setupDeployments();

		orchestratorStateKey = RuntimeParameter.constructParamName(
				Run.constructOrchestratorName(cloudServiceName),
				RuntimeParameter.STATE_KEY);

		String nodeName = deployment.getNodes().get("node1").getName();

		node1StateKey = RuntimeParameter.constructParamName(nodeName, 1,
				RuntimeParameter.STATE_KEY);

		node1CompleteKey = RuntimeParameter.constructParamName(nodeName, 1,
				RuntimeParameter.COMPLETE_KEY);

		nodeName = deployment.getNodes().get("node2").getName();

		node2StateKey = RuntimeParameter.constructParamName(nodeName, 1,
				RuntimeParameter.STATE_KEY);

		node2CompleteKey = RuntimeParameter.constructParamName(nodeName, 1,
				RuntimeParameter.COMPLETE_KEY);

		createUser();
	}

	@After
	public void tearDown() {
		removeDeployments();
	}

	@Test
	public void completeCurrentState() throws FileNotFoundException,
			IOException, SlipStreamException {
		Run run = RunFactory.getRun(deployment, RunType.Orchestration,
				cloudServiceName, user);
		run = run.store();

		States newState = States.Inactive;

		assertState(run, States.Inactive, newState);

		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.Initializing, newState);

		newState = completeCurrentState(node1CompleteKey, run);
		newState = completeCurrentState(node2CompleteKey, run);

		assertState(run, States.Initializing, newState);

		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.Running, newState);

		newState = completeCurrentState(node1CompleteKey, run);
		newState = completeCurrentState(node2CompleteKey, run);

		assertState(run, States.Running, newState);

		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.SendingFinalReport, newState);

		newState = completeCurrentState(node1CompleteKey, run);
		newState = completeCurrentState(node2CompleteKey, run);

		assertState(run, States.SendingFinalReport, newState);

		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.Finalizing, newState);

		newState = completeCurrentState(node1CompleteKey, run);
		newState = completeCurrentState(node2CompleteKey, run);

		assertState(run, States.Finalizing, newState);

		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.Terminal, newState);

		newState = completeCurrentState(node1CompleteKey, run,
				Status.CLIENT_ERROR_CONFLICT);
		newState = completeCurrentState(orchestratorStateKey, run,
				Status.CLIENT_ERROR_CONFLICT);

		run.remove();
	}

	private States completeCurrentState(String key, Run run)
			throws ConfigurationException {
		return completeCurrentState(key, run, Status.SUCCESS_OK);
	}

	private States completeCurrentState(String key, Run run, Status expected)
			throws ConfigurationException {
		Request request = createPostRequest(run.getUuid(), key,
				new StringRepresentation(""));
		Response response = executeRequest(request);

		assertEquals(expected, response.getStatus());
		String state = response.getEntityAsText();
		return state == null ? States.Unknown : States.valueOf(state);
	}

	private void assertState(Run run, States expectedState,
			States effectiveState) {
		String globalStateKey = RuntimeParameter.GLOBAL_STATE_KEY;

		assertThat(effectiveState, is(expectedState));

		RuntimeParameter state = RuntimeParameter.loadFromUuidAndKey(
				run.getUuid(), globalStateKey);
		assertThat(state.getValue(), is(expectedState.toString()));

		state = RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
				orchestratorStateKey);
		assertThat(state.getValue(), is(expectedState.toString()));

		state = RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
				node1StateKey);
		assertThat(state.getValue(), is(expectedState.toString()));

		state = RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
				node2StateKey);
		assertThat(state.getValue(), is(expectedState.toString()));

	}
}
