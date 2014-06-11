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
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.statemachine.States;
import com.sixsq.slipstream.util.CommonTestUtil;

public class RuntimeParameterStateMachineTest extends
		RuntimeParameterResourceTestBase {

	private static String node1CompleteKey;
	private static String node2CompleteKey;
	private static String orchestratorCompleteKey;

	@Before
	public void setupClass() throws ValidationException, NotFoundException {
		setupDeployments();

		orchestratorCompleteKey = RuntimeParameter.constructParamName(
				Run.constructOrchestratorName(cloudServiceName),
				RuntimeParameter.COMPLETE_KEY);

		String nodeName = deployment.getNodes().get("node1").getName();
		node1CompleteKey = RuntimeParameter.constructParamName(nodeName, 1,
				RuntimeParameter.COMPLETE_KEY);

		nodeName = deployment.getNodes().get("node2").getName();
		node2CompleteKey = RuntimeParameter.constructParamName(nodeName, 1,
				RuntimeParameter.COMPLETE_KEY);

		createUser();
		CommonTestUtil.addSshKeys(user);
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

		States newState = States.Initializing;

		assertState(run, States.Initializing, newState);

		newState = completeCurrentState(orchestratorCompleteKey, run);

		assertState(run, States.Provisioning, newState);

		newState = completeCurrentState(node1CompleteKey, run);
		newState = completeCurrentState(node2CompleteKey, run);

		assertState(run, States.Provisioning, newState);

		newState = completeCurrentState(orchestratorCompleteKey, run);

		assertState(run, States.Executing, newState);

		newState = completeCurrentState(node1CompleteKey, run);
		newState = completeCurrentState(node2CompleteKey, run);

		assertState(run, States.Executing, newState);

		newState = completeCurrentState(orchestratorCompleteKey, run);

		assertState(run, States.SendingReports, newState);

		newState = completeCurrentState(node1CompleteKey, run);
		newState = completeCurrentState(node2CompleteKey, run);

		assertState(run, States.SendingReports, newState);

		newState = completeCurrentState(orchestratorCompleteKey, run);

		assertState(run, States.Ready, newState);

		newState = completeCurrentState(node1CompleteKey, run);
		newState = completeCurrentState(node2CompleteKey, run);

		assertState(run, States.Ready, newState);

		newState = completeCurrentState(orchestratorCompleteKey, run);
		
		
		assertState(run, States.Finalizing, newState);

		//newState = completeCurrentState(node1CompleteKey, run);
		//newState = completeCurrentState(node2CompleteKey, run);

		//assertState(run, States.Finalizing, newState);

		newState = completeCurrentState(orchestratorCompleteKey, run);
		

		assertState(run, States.Done, newState);

		newState = completeCurrentState(node1CompleteKey, run,
				Status.CLIENT_ERROR_CONFLICT);
		newState = completeCurrentState(orchestratorCompleteKey, run,
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
	}
}
