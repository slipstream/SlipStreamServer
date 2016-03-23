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
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.io.IOException;
import java.util.logging.Logger;

public class RuntimeParameterResource extends RunBaseResource {

	private RuntimeParameter runtimeParameter;

	private String key;

	@Override
	public void initializeSubResource() throws ResourceException {

		long start = System.currentTimeMillis();
		long before;

		before = System.currentTimeMillis();
		parseRequest();
		logTimeDiff("parseRequest", before);

		before = System.currentTimeMillis();
		fetchRepresentation();
		logTimeDiff("fetchRepresentation", before);

		before = System.currentTimeMillis();
		raiseConflictIfAbortIsSet();
		logTimeDiff("raiseConflictIfAbortIsSet", before);

		logTimeDiff("initialize on runtime parameter", start);
	}

	private void parseRequest() {
		extractAndSetIgnoreAbort();

		key = getAttribute("key");
	}

	private void fetchRepresentation() {
		runtimeParameter = loadRuntimeParameter(key);
		setExisting(runtimeParameter != null);
	}

	private void raiseConflictIfAbortIsSet() {
		if (!getIgnoreAbort()) {
			if (isAbortSet()) {
				throw (new ResourceException(Status.CLIENT_ERROR_CONFLICT,
						"Abort flag raised!"));
			}
		}
	}

	private void abortOrReset(String abortMessage, EntityManager em) {
		String nodename = RuntimeParameter.extractNodeNamePart(key);
		Run.abortOrReset(abortMessage, nodename, em, getUuid());
	}


	@Delete
	public void resetRuntimeParameter() throws ResourceException {

		runtimeParameter.setValue("");
		runtimeParameter.setIsSet(false);
		runtimeParameter.store();

		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		try {
			transaction.begin();
			abortOrReset(null, em);
			transaction.commit();
		} finally {
			em.close();
		}

		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Get
	public String represent() throws ResourceException, NotFoundException,
			ValidationException {

		long start = System.currentTimeMillis();

		if (!runtimeParameter.isSet()) {
			throw new ResourceException(
					Status.CLIENT_ERROR_PRECONDITION_FAILED, "key " + key
							+ " not yet set");
		}

		logTimeDiff("processing get on runtime parameter", start);
		return runtimeParameter.getValue();
	}

	private String truncateMiddle(int maxLength, String text, String truncateMessage) {
		if (text != null && text.length() > maxLength) {
			int partsize = (maxLength - truncateMessage.length()) / 2;
			text = text.substring(0, partsize-1) + truncateMessage + text.substring(text.length() - partsize);
		}
		return text;
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
			if (!runtimeParameter.isSet()) {
				value = truncateMiddle(RuntimeParameter.VALUE_MAX_LENGTH, value, "\n(truncated)\n");
				abortOrReset(value, em);
				setValue(value);
			}
		} else if (isGlobalStateParameter()) {
			States newState = attemptChangeGlobalState(value);
			setValue(newState.toString());
		} else {
			setValue(value);
		}

		transaction.commit();
		em.close();
		getResponse().setEntity(null, MediaType.ALL);
	}

	private void setValue(String value) {
		if (value != null && value.length() > RuntimeParameter.VALUE_MAX_LENGTH) {
			throwClientBadRequest("Value too long (" + value.length() + "). " +
					"Maximum length allowed: " + RuntimeParameter.VALUE_MAX_LENGTH + ".");
		}
		runtimeParameter.setValue(value);
		RuntimeParameterMediator.processSpecialValue(runtimeParameter);
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

	private States attemptChangeGlobalState(String toState) {
		States toNewState = null;
		try {
			toNewState = States.valueOf(toState);
		} catch (IllegalArgumentException e) {
			throwClientBadRequest("Requested transition to unknown state: " + toState);
		}
		States newState = null;
		// Jumping between states with an abort flag set on the run is not a good practice.
		if (isAbortSet()) {
			throwClientBadRequest("For any state transition via API abort should be cleared first.");
		} else {
			Run run = Run.loadFromUuid(getUuid());
			States currentState = run.getState();
			if (run.isMutable() && States.Ready == currentState && States.Provisioning == toNewState) {
				newState = attemptChangeGlobalStateToProvisioning();
			} else if (getUser().isSuper()) {
    			newState = toNewState;
			} else {
				throwClientBadRequest(String.format(
				        "Via API state can be advanced only on a mutable run and only from %s to %s", States.Ready,
				        States.Provisioning));
			}
		}
		return newState;
	}

	private States attemptChangeGlobalStateToProvisioning() {
		String fromToState = String.format("from %s to %s", States.Ready, States.Provisioning);

		StateMachine sm;
		EntityManager em = PersistenceUtil.createEntityManager();
		Run run = Run.loadFromUuid(getUuid(), em);
		EntityTransaction transaction = em.getTransaction();
		try {
			transaction.begin();
			sm = StateMachine.createStateMachine(run);
			try {
				sm.tryAdvanceToProvisionning();
			} catch (InvalidStateException | CannotAdvanceFromTerminalStateException e) {
				e.printStackTrace();
				throwClientBadRequest(String.format("Failed to advance state %s: %s", fromToState, e.getMessage()));
			}
			transaction.commit();
		} catch (Exception ex) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw ex;
		} finally {
			em.close();
		}
		States newState = sm.getState();
		if (States.Provisioning != newState) {
			throwServerError(String.format("Failed to advance state %s: requested doesn't match reached %s.",
					fromToState, newState));
		}
		return newState;
	}

	@Post
	public void completeCurrentNodeStateOrChangeGlobalState(Representation entity) {
		String nodeName = runtimeParameter.getNodeName();
		States newState = attemptCompleteCurrentNodeState(nodeName);
		getResponse().setEntity(newState.toString(), MediaType.TEXT_PLAIN);
	}


	private States attemptCompleteCurrentNodeState(String nodeName) {
		StateMachine sm = createStateMachine();
		return completeCurrentNodeState(nodeName, sm);
	}

	public States completeCurrentNodeState(String nodeName, StateMachine sc) {
		States state;
		try {
			state = sc.updateState(nodeName);
		} catch (InvalidStateException | SlipStreamClientException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, e.getMessage());
		}
		return state;
	}

	@Override
	protected String getPageRepresentation() {
		// TODO Stub
		return null;
	}

	private void logTimeDiff(String msg, long before, long after) {
		Logger.getLogger("Timing").finest("took to execute " + msg + ": " + (after - before));
	}

	protected void logTimeDiff(String msg, long before) {
		logTimeDiff(msg, before, System.currentTimeMillis());
	}

	private boolean isGlobalStateParameter() { 
		return RuntimeParameter.GLOBAL_STATE_KEY.equals(key); 
	}

	private StateMachine createStateMachine() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Run run = Run.loadFromUuid(getUuid(), em);
		StateMachine sm = StateMachine.createStateMachine(run);
		em.close();
		return sm;
	}
}
