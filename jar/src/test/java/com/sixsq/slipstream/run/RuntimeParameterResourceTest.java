package com.sixsq.slipstream.run;

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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.statemachine.States;

public class RuntimeParameterResourceTest extends
		RuntimeParameterResourceTestBase {

	@Test
	public void runtimeParameterResourceGetUnknownUuid()
			throws ConfigurationException {

		Request request = createGetRequest("unknownUuid", "aKey");

		Response response = executeRequest(request);

		assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
	}

	@Test
	public void runtimeParameterResourceGetUnknownKey()
			throws FileNotFoundException, IOException, SlipStreamException {

		Run run = createAndStoreRunImage("RuntimeParameterResourceGetUnknownKey");

		Request request = createGetRequest(run.getUuid(), "unknownKey");

		Response response = executeRequest(request);

		assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());

		run = Run.loadFromUuid(run.getUuid());
		assertAbortSet(run);

		run.remove();
	}

	@Test
	public void runtimeParameterResourceGetNotYetSetValue()
			throws FileNotFoundException, IOException, SlipStreamException {

		Run run = createAndStoreRunImage("runtimeParameterResourceGetNotYetSetValue");

		Request request = createGetRequest(run.getUuid(), "ss:abort");

		Response response = executeRequest(request);

		assertEquals(Status.CLIENT_ERROR_PRECONDITION_FAILED,
				response.getStatus());

		assertAbortNotSet(run);

		run.remove();
	}

	@Test
	public void runtimeParameterResourceGet() throws FileNotFoundException,
			IOException, SlipStreamException {

		Run run = createAndStoreRunImage("RuntimeParameterResourceGet");

		String key = "node.1:key";
		String value = "value of key";
		storeRuntimeParameter(key, value, run);

		executeGetRequestAndAssertValue(run, key, value);

		run.remove();
	}

	private void storeRuntimeParameter(String key, String value, Run run)
			throws ValidationException, NotFoundException {
		run.assignRuntimeParameter(key, value, "");
		run.store();
	}

	@Test
	public void runtimeParameterResourcePutExisting()
			throws FileNotFoundException, IOException, SlipStreamException {

		Run run = createAndStoreRunImage("runtimeParameterResourcePutExisting");

		String key = "node.1:key";
		String value = "value of key";

		run.assignRuntimeParameter(key, value, "");
		run.store();

		Request request = createPutRequest(run.getUuid(), key,
				new StringRepresentation(value));

		executeRequest(request);

		RuntimeParameter runtimeParameter = RuntimeParameter
				.loadFromUuidAndKey(run.getUuid(), key);

		run.remove();

		assertNotNull(runtimeParameter);
		assertEquals("value of key", runtimeParameter.getValue());

		assertAbortNotSet(run);
	}

	private void assertAbortNotSet(Run run) {
		assertThat(run.getRuntimeParameters().get("ss:abort").isSet(),
				is(false));
	}

	private void assertAbortSet(Run run) {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		run = em.merge(run);

		assertThat(run.getRuntimeParameters().get("ss:abort").isSet(), is(true));

		transaction.commit();
		em.close();
	}

	@Test
	public void runtimeParameterResourcePutNotExisting()
			throws FileNotFoundException, IOException, SlipStreamException {

		Run run = createAndStoreRunImage("runtimeParameterResourcePutNotExisting");

		String key = "node.1:key";
		Form form = new Form();
		String value = "value of key";
		form.add("value", value);

		Representation entity = form.getWebRepresentation();
		Request request = createPutRequest(run.getUuid(), key, entity);

		Response response = executeRequest(request);

		run.remove();

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}

	@Test
	public void runtimeParameterRetrieveFromContainerRun()
			throws FileNotFoundException, IOException, SlipStreamException {

		Run run = createAndStoreRunImage("RuntimeParameterRetrieveFromContainerRun");

		String key = "node.1:key";
		String value = "value of key";
		storeRuntimeParameter(key, value, run);

		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();

		run = Run.loadFromUuid(run.getUuid(), em);

		assertNotNull(run);
		assertEquals("value of key", run.getRuntimeParameterValue(key));

		transaction.commit();
		em.close();

		run.remove();
	}

	@Test
	public void runtimeParameterReset() throws SlipStreamException,
			FileNotFoundException, IOException {

		String key = "node.1:key";
		String value = "value of key";
		Run run = createAndStoreRunWithRuntimeParameter(
				"runtimeParameterReset", key, value);

		assertNotNull(run);
		assertEquals(value, run.getRuntimeParameterValue(key));

		Request request = createDeleteRequest(run.getUuid(), key);
		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_OK, response.getStatus());

		assertRuntimeParameterWasReset(run, key);

		run.remove();
	}

	private void assertRuntimeParameterWasReset(Run run, String key)
			throws AbortException {
		RuntimeParameter rtp = RuntimeParameter.loadFromUuidAndKey(
				run.getUuid(), key);
		assertNotNull(rtp);
		assertThat(rtp.isSet(), is(false));
	}

	@Test
	public void wrongNodeTriggersAbort() throws SlipStreamException,
			FileNotFoundException, IOException {

		String key = "wrong.1:key";
		Run run = createAndStoreRunImage("wrongNodeTriggersAbort");

		Request request = createGetRequest(run.getUuid(), key);
		Response response = executeRequest(request);

		assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());

		run = Run.loadFromUuid(run.getUuid());
		assertAbortSet(run);

		run.remove();
	}

	@Test
	public void wrongKeyTriggersAbort() throws SlipStreamException,
			FileNotFoundException, IOException {

		String key = "ss:wrong";
		Run run = createAndStoreRunImage("wrongKeyTriggersAbort");

		Request request = createGetRequest(run.getUuid(), key);
		Response response = executeRequest(request);

		assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());

		run = Run.loadFromUuid(run.getUuid());
		assertAbortSet(run);

		run.remove();
	}

	@Test
	public void cantSetAbortTwice() throws SlipStreamException,
			FileNotFoundException, IOException {

		String key = "ss:abort";
		Run run = createAndStoreRunImage("cantSetAbortTwice");

		Request request = createPutRequest(run.getUuid(), key,
				new StringRepresentation("first abort"));
		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_OK, response.getStatus());

		RuntimeParameter abort = RuntimeParameter.loadFromUuidAndKey(
				run.getUuid(), "ss:abort");
		assertThat(abort.getValue(), is("first abort"));

		request = createPutRequest(run.getUuid(), key,
				new StringRepresentation("second abort"));
		response = executeRequest(request);

		assertEquals(Status.CLIENT_ERROR_CONFLICT, response.getStatus());

		abort = RuntimeParameter.loadFromUuidAndKey(run.getUuid(), "ss:abort");
		assertThat(abort.getValue(), is("first abort"));

		run.remove();
	}

	@Test
	public void errorSetsNodeAndGlobalAbort() throws FileNotFoundException,
			IOException, SlipStreamException {
		String key = "orchestrator:abort";
		Run run = createAndStoreRunImage("errorSetsNodeAndGlobalAbort");

		Request request = createPutRequest(run.getUuid(), key,
				new StringRepresentation("orchestrator abort"));
		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_OK, response.getStatus());

		RuntimeParameter nodeAbort = RuntimeParameter.loadFromUuidAndKey(
				run.getUuid(), "orchestrator:abort");
		assertThat(nodeAbort.getValue(), is("orchestrator abort"));

		RuntimeParameter globalAbort = RuntimeParameter.loadFromUuidAndKey(
				run.getUuid(), "ss:abort");
		assertThat(globalAbort.getValue(), is("orchestrator abort"));
	}

	@Test
	public void cancelAbort() throws FileNotFoundException, IOException,
			SlipStreamException {

		String key = "orchestrator:abort";
		Run run = createAndStoreRunImage("errorSetsNodeAndGlobalAbort");

		Request request = createPutRequest(run.getUuid(), key,
				new StringRepresentation("orchestrator abort"));
		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_OK, response.getStatus());

		RuntimeParameter nodeAbort = RuntimeParameter.loadFromUuidAndKey(
				run.getUuid(), "orchestrator:abort");
		assertThat(nodeAbort.getValue(), is("orchestrator abort"));

		RuntimeParameter globalAbort = RuntimeParameter.loadFromUuidAndKey(
				run.getUuid(), "ss:abort");
		assertThat(globalAbort.getValue(), is("orchestrator abort"));

		Map<String, Object> attributes = createRequestAttributes(run.getUuid(),
				key);
		attributes.put("ignoreabort", "true");
		request = createPutRequest(attributes, new StringRepresentation(""));

		response = executeRequest(request);

		assertEquals(Status.SUCCESS_OK, response.getStatus());

		nodeAbort = RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
				"orchestrator:abort");
		assertThat(nodeAbort.getValue(), is(""));
		globalAbort = RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
				"ss:abort");
		assertThat(globalAbort.getValue(), is(""));
	}

	@Test
	public void completeCurrentState() throws FileNotFoundException,
			IOException, SlipStreamException {
		Run run = createAndStoreRunImage("completeCurrentState");
		States newState = States.Inactive;
		String orchestratorStateKey = Run.ORCHESTRATOR_NAME_PREFIX
				+ RuntimeParameter.STATE_KEY;
		String machineStateKey = Run.MACHINE_NAME_PREFIX
				+ RuntimeParameter.STATE_KEY;
		String machineCompleteKey = Run.MACHINE_NAME_PREFIX
				+ RuntimeParameter.COMPLETE_KEY;

		assertState(run, States.Inactive, newState);

		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.Initializing, newState);

		newState = completeCurrentState(machineStateKey, run);

		assertState(run, States.Initializing, newState);

		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.Running, newState);

		newState = completeCurrentState(machineStateKey, run);

		assertState(run, States.Running, newState);

		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.SendingFinalReport, newState);

		newState = completeCurrentState(machineCompleteKey, run);

		assertState(run, States.SendingFinalReport, newState);

		newState = completeCurrentState(machineCompleteKey, run);
		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.Finalizing, newState);

		newState = completeCurrentState(machineCompleteKey, run);
		newState = completeCurrentState(orchestratorStateKey, run);

		assertState(run, States.Terminal, newState);

		newState = completeCurrentState(machineCompleteKey, run, Status.CLIENT_ERROR_CONFLICT);
		newState = completeCurrentState(orchestratorStateKey, run, Status.CLIENT_ERROR_CONFLICT);
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
		String orchestratorStateKey = Run.ORCHESTRATOR_NAME_PREFIX
				+ RuntimeParameter.STATE_KEY;
		String machineStateKey = Run.MACHINE_NAME_PREFIX
				+ RuntimeParameter.STATE_KEY;

		assertThat(effectiveState, is(expectedState));

		RuntimeParameter state = RuntimeParameter.loadFromUuidAndKey(
				run.getUuid(), globalStateKey);
		assertThat(state.getValue(), is(expectedState.toString()));

		state = RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
				orchestratorStateKey);
		assertThat(state.getValue(), is(expectedState.toString()));

		state = RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
				machineStateKey);
		assertThat(state.getValue(), is(expectedState.toString()));

	}
}
