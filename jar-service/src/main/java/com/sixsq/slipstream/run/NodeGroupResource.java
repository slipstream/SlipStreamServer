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
import java.util.Map;

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
import com.sixsq.slipstream.statemachine.StateMachine;
import com.sixsq.slipstream.statemachine.States;

public class NodeGroupResource extends ServerResource {

	private String uuid;

	private String node;

	private NodeGroup nodeGroup;

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
		node = attributes.get("node").toString();
	}

	private void parseRequestQuery() {
		extractAndSetIgnoreAbort();
	}

	private void extractAndSetIgnoreAbort() {
		ignoreAbort = getRequest().getAttributes().containsKey(
				RunListResource.IGNORE_ABORT_QUERY);
	}

	private void fetchRepresentation() {
		loadNodeGroup();
		setExisting(nodeGroup != null);
	}

	private void loadNodeGroup() {
		nodeGroup = NodeGroup.loadFromUuidAndKey(uuid, node);
		if (nodeGroup == null) {
			Run run = Run.loadFromUuid(uuid);
			if (run == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"Unknown run id " + uuid);
			} else {
				String error = "Unknown key " + node;
				String nodename = RuntimeParameter.extractNodeNamePart(node);
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
		String nodename = RuntimeParameter.extractNodeNamePart(node);
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

		if (RuntimeParameter.GLOBAL_ABORT_KEY.equals(node)) {
			nodeGroup.setValue("");
		}

		nodeGroup.setIsSet(false);
		nodeGroup.store();

		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Get
	public String represent() throws ResourceException, NotFoundException,
			ValidationException {

		if (!nodeGroup.isSet()) {
			throw new ResourceException(
					Status.CLIENT_ERROR_PRECONDITION_FAILED, "key " + node
							+ " not yet set");
		}
		return nodeGroup.getValue();
	}

	@Put
	public void update(Representation entity) throws ResourceException,
			NotFoundException, ValidationException {

		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		nodeGroup = em.merge(nodeGroup);

		String value = (entity == null ? "" : extractValueFromEntity(entity));

		boolean isGlobalAbort = RuntimeParameter.GLOBAL_ABORT_KEY.equals(node);
		boolean isNodeAbort = (nodeGroup.getNodeName()
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR + RuntimeParameter.ABORT_KEY)
				.equals(node);

		if (isGlobalAbort || isNodeAbort) {
			if (ignoreAbort || !nodeGroup.isSet()) {
				abortOrReset(value, em);
				nodeGroup.setValue(value);
			}
		} else {
			nodeGroup.setValue(value);
		}

		transaction.commit();
		em.close();
		getResponse().setEntity(null, MediaType.ALL);
	}

	private String extractValueFromEntity(Representation entity) {
		try {
			return entity.getText();
		} catch (IOException e) {
			throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Bad value for key: " + node + " with details: "
							+ e.getMessage()));
		}
	}

	@Post
	public void completeCurrentNodeState(Representation entity) {

		String nodeName = nodeGroup.getNodeName();
		States newState = null;

		newState = attemptCompleteCurrentNodeState(nodeName);

		getResponse().setEntity(newState.toString(), MediaType.TEXT_PLAIN);
	}

	private States attemptCompleteCurrentNodeState(String nodeName) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Run run = Run.loadFromUuid(uuid, em);
		StateMachine sc = StateMachine.createStateMachine(run);
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

}
