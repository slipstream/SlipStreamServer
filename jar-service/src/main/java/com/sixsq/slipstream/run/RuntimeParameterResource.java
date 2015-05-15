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
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import com.sixsq.slipstream.exceptions.*;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.statemachine.StateMachine;
import com.sixsq.slipstream.statemachine.States;

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
				abortOrReset(value, em);
				setValue(value);
			}
		} else {
			setValue(value);
		}

		transaction.commit();
		em.close();
		getResponse().setEntity(null, MediaType.ALL);
	}

	private void setValue(String value) {
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

	@Post
	public void completeCurrentNodeState(Representation entity) {

        com.sixsq.slipstream.util.Logger.info("OLD STATE: " + runtimeParameter.getContainer().getState());

        if(!runtimeParameter.getName().equals(RuntimeParameter.COMPLETE_KEY)) {
            throwClientConflicError("Only " + RuntimeParameter.COMPLETE_KEY + " key can be posted");
        }
        runtimeParameter.setValue("true");
        runtimeParameter.store();
//		com.sixsq.slipstream.util.Logger.info(">>>>>>>>>>> Before: " + nodeName);
		States newState = null;
		EntityManager em = PersistenceUtil.createEntityManager();
		runtimeParameter = (RuntimeParameter) RuntimeParameter.load(runtimeParameter.getResourceUri(), RuntimeParameter.class, em);//em.merge(runtimeParameter);

        String nodeName = runtimeParameter.getNodeName();
		try {
			newState = attemptCompleteCurrentNodeStateWithRetry(nodeName, em, null, true);
            em.close();;
            com.sixsq.slipstream.util.Logger.info("NEW STATE: " + newState);
            com.sixsq.slipstream.util.Logger.info("Closed!!");
		} catch (SlipStreamDatabaseException e) {
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error in state machine");
		}

		RuntimeParameter v = RuntimeParameter.load(runtimeParameter.getResourceUri());
		com.sixsq.slipstream.util.Logger.info("################ RP: " + v.getNodeName() + " with value: " + v.getValue() + " and state: " + v.getContainer().getState());

		getResponse().setEntity(newState.toString(), MediaType.TEXT_PLAIN);
//		com.sixsq.slipstream.util.Logger.info("<<<<<<<<<<< After: " + nodeName);
	}

	private States attemptCompleteCurrentNodeStateWithRetry(String nodeName, EntityManager em, StateMachine sc, boolean retry) {
        com.sixsq.slipstream.util.Logger.info("################ RP: " + nodeName);
        Run run = runtimeParameter.getContainer();
        run = em.find(Run.class, run.getResourceUri());
//		runtimeParameter = em.merge(runtimeParameter);
//		if(sc == null) {
			sc = StateMachine.createStateMachine(run);
//		}
		try {
			return attemptCompleteCurrentNodeState(nodeName, em, sc);
		} catch (PersistenceException e) {
			e.printStackTrace();
			if(retry) {
				com.sixsq.slipstream.util.Logger.info("Failed completing node... retrying!");
//				runtimeParameter = em.merge(runtimeParameter);
//				return attemptCompleteCurrentNodeStateWithRetry(nodeName, em, null, true);
			} else {
				throw e;
			}
		}
        return run.getState();
	}

	private States attemptCompleteCurrentNodeState(String nodeName, EntityManager em, StateMachine sc) {
		em.getTransaction().begin();
		States state = completeCurrentNodeState(nodeName, sc);
		try {
			em.getTransaction().commit();
            com.sixsq.slipstream.util.Logger.info("Committed!!");
		} catch (PersistenceException e) {
			if(em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			throw e;
		}

		return state;
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
}
