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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.sixsq.slipstream.exceptions.CannotAdvanceFromTerminalStateException;
import com.sixsq.slipstream.exceptions.InvalidStateException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.statemachine.ExtrinsicState;
import com.sixsq.slipstream.statemachine.State;
import com.sixsq.slipstream.statemachine.StateFactory;
import com.sixsq.slipstream.statemachine.StateMachine;
import com.sixsq.slipstream.statemachine.States;

public class RuntimeParameterResource extends ServerResource {

	private String uuid;

	private RuntimeParameter runtimeParameter;

	private String key;

	private boolean ignoreAbort;

	@Override
	public void doInit() throws ResourceException {

		parseRequest();

		fetchRepresentation();

		raiseConflictIfAbortIsSet();
	}

	private void parseRequest() {

		parseRequestQuery();

		Map<String, Object> attributes = getRequest().getAttributes();
		uuid = attributes.get("uuid").toString();
		key = attributes.get("key").toString();
	}

	private void parseRequestQuery() {
		extractAndSetIgnoreAbort();
	}

	private void extractAndSetIgnoreAbort() {
		ignoreAbort = getRequest().getAttributes().containsKey(
				RunListResource.IGNORE_ABORT_QUERY);
	}

	private void fetchRepresentation() {
		loadRuntimeParameter();
		setExisting(runtimeParameter != null);
	}

	private void loadRuntimeParameter() {
		runtimeParameter = RuntimeParameter.loadFromUuidAndKey(uuid, key);
		if (runtimeParameter == null) {
			Run run = Run.loadFromUuid(uuid);
			if (run == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"Unknown run id " + uuid);
			} else {
				String error = "Unknown key " + key;
				String nodename = RuntimeParameter.extractNodeNamePart(key);
				Run.abortOrReset(error, nodename, uuid);
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						error);
			}
		}
	}

	private void raiseConflictIfAbortIsSet() {
		if (!ignoreAbort) {
			if (isAbortSet()) {
				throw (new ResourceException(Status.CLIENT_ERROR_CONFLICT,
						"Abort flag raised!"));
			}
		}
	}

	private void abortOrReset(String abortMessage, EntityManager em) {
		String nodename = RuntimeParameter.extractNodeNamePart(key);
		Run.abortOrReset(abortMessage, nodename, em, uuid);

	}

	private RuntimeParameter getGlobalAbort() {
		RuntimeParameter abort = RuntimeParameter.loadFromUuidAndKey(uuid,
				RuntimeParameter.GLOBAL_ABORT_KEY);
		return abort;
	}

	private boolean isAbortSet() {
		return getGlobalAbort().isSet();
	}

	@Delete
	public void resetRuntimeParameter() throws ResourceException {

		if (RuntimeParameter.GLOBAL_ABORT_KEY.equals(key)) {
			runtimeParameter.setValue("");
		}

		runtimeParameter.setIsSet(false);
		runtimeParameter.store();

		getResponse().setStatus(Status.SUCCESS_OK);
	}

	@Get
	public String represent() throws ResourceException, NotFoundException,
			ValidationException {

		if (!runtimeParameter.isSet()) {
			throw new ResourceException(
					Status.CLIENT_ERROR_PRECONDITION_FAILED, "key " + key
							+ " not yet set");
		}
		return runtimeParameter.getValue();
	}

	@Put
	public void update(Representation entity) throws ResourceException,
			NotFoundException, ValidationException {

		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		runtimeParameter = em.merge(runtimeParameter);

		String value = (entity == null ? "" : extractValueFromEntity(entity));

		boolean isGlobalAbort = RuntimeParameter.GLOBAL_ABORT_KEY.equals(key);
		boolean isNodeAbort = (runtimeParameter.getNodeName()
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR + RuntimeParameter.ABORT_KEY)
				.equals(key);

		if (isGlobalAbort || isNodeAbort) {
			if (ignoreAbort || !runtimeParameter.isSet()) {
				abortOrReset(value, em);
				runtimeParameter.setValue(value);
			}
		} else {
			runtimeParameter.setValue(value);
		}

		transaction.commit();
		em.close();
	}

	private String extractValueFromEntity(Representation entity) {
		try {
			return entity.getText();
		} catch (IOException e) {
			throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Bad value for key: " + key + " with details: "
							+ e.getMessage()));
		}
	}

	@Post
	public void completeCurrentNodeState(Representation entity) {

		String nodeName = runtimeParameter.getNodeName();
		States newState = null;

		newState = attemptCompleteCurrentNodeState(nodeName);

		updateRunState(newState, true);

		getResponse().setEntity(newState.toString(), MediaType.TEXT_PLAIN);

	}

	private void updateRunState(States newState, boolean retry) {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		try {
			Run run = Run.loadFromUuid(uuid, em);
			run.setState(newState);
			run.store();
			transaction.commit();
			em.close();
		} catch (Exception e) {
			String error = "error setting run state: " + newState;
			if (retry) {
				Logger.getLogger("restlet").warning(error + " retrying...");
			} else {
				Logger.getLogger("restlet").severe(error);
			}
			// retry once
			if (retry) {
				updateRunState(newState, false);
			}
		}
	}

	private States attemptCompleteCurrentNodeState(String nodeName) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Run run = Run.loadFromUuid(uuid, em);
		StateMachine sc = createStateMachine(run);
		em.close();
		return completeCurrentNodeState(nodeName, sc);
	}

	public States completeCurrentNodeState(String nodeName, StateMachine sc) {
		States state;
		try {
			state = sc.updateState(nodeName);
		} catch (CannotAdvanceFromTerminalStateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		} catch (InvalidStateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		} catch (SlipStreamClientException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		}
		return state;
	}

	protected StateMachine createStateMachine(Run run) {
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

	private StateMachine getStateMachine(Run run) throws ValidationException,
			InvalidStateException, NotFoundException {
		String nodeNames = run.getNodeNames();
		String[] nodes = nodeNames.split(", ");

		Map<String, State> nodeStates = new HashMap<String, State>();
		String key;
		for (String node : nodes) {
			String nodePrefix = node + RuntimeParameter.NODE_PROPERTY_SEPARATOR;
			key = nodePrefix + RuntimeParameter.COMPLETE_KEY;
			RuntimeParameter completed = run.getRuntimeParameters().get(key);

			key = nodePrefix + RuntimeParameter.ABORT_KEY;
			RuntimeParameter failing = run.getRuntimeParameters().get(key);

			RuntimeParameter state = run.getRuntimeParameters().get(
					nodePrefix + RuntimeParameter.STATE_KEY);

			ExtrinsicState extrinsicState = new ExtrinsicState(completed,
					failing, state);
			State nodeState = StateFactory.createInstance(extrinsicState);

			nodeStates.put(node, nodeState);
		}

		RuntimeParameter globalFailing = run.getRuntimeParameters().get(
				RuntimeParameter.GLOBAL_ABORT_KEY);
		RuntimeParameter globalState = run.getRuntimeParameters().get(
				RuntimeParameter.GLOBAL_STATE_KEY);

		ExtrinsicState globalExtrinsicState = new ExtrinsicState(null,
				globalFailing, globalState);
		State globalNodeState = StateFactory
				.createInstance(globalExtrinsicState);

		StateMachine sc = new StateMachine(nodeStates, globalNodeState);

		return sc;
	}

}
