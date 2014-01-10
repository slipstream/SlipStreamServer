package com.sixsq.slipstream.action;

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
import java.sql.Timestamp;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;

import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.representation.Representation;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.persistence.PersistenceUtil;

/**
 * Parent class for one shot, asynchronous actions. These include such things as
 * a user confirming an email address or an administrator validating an account.
 * 
 * Subclasses must call the method setForm only from the constructor.
 * 
 */

// TODO: split the persistent part from the resource
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class OneShotAction {

	public enum State {
		ACTIVE, DONE, ABORTED
	}

	@Id
	String uuid;

	@Enumerated(EnumType.STRING)
	private State state;

	private Timestamp lastModified;

	@Lob
	private String encodedForm;

	public OneShotAction() {
		this.uuid = UUID.randomUUID().toString();
		this.state = State.ACTIVE;

		encodedForm = null;

		setLastModified();
	}

	public String getUuid() {
		return uuid;
	}

	public State getState() {
		return state;
	}

	public void setState(State newState) {
		if (State.ACTIVE.equals(state)) {
			if (!State.ACTIVE.equals(newState)) {
				state = newState;
				setLastModified();
			}
		}
	}

	public Timestamp getLastModified() {
		return lastModified;
	}

	protected void setLastModified() {
		lastModified = new Timestamp(System.currentTimeMillis());
	}

	public Form getForm() {
		Form form = new Form(encodedForm);
		return form;
	}

	/**
	 * Method to save the state necessary to perform the action at some point in
	 * the future. This method should only be called once from the constructor.
	 * Once the form information has been set, it cannot be modified.
	 * 
	 * @param form
	 */
	protected void setForm(Form form) {
		try {
			if (encodedForm == null) {
				if (form == null) {
					form = new Form();
				}
				encodedForm = form.encode();
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	/**
	 * If the URL for this action accessed, then this method will be called to
	 * execute the given action. The command embedded in the URL will be passed
	 * as the parameter. The semantics of the command are defined by the
	 * subclass. Before the method completes, it must update the state
	 * information setting it to DONE or ABORTED. It may leave the state as
	 * ACTIVE, if the action couldn't be done for some reason and it makes sense
	 * to keep it accessible.
	 * 
	 * The command used in the URL is available through the request attributes
	 * with the key "command". The service configuration information is
	 * available through the key "org.sixsq.slipstream.Configuration".
	 * 
	 * @param request
	 * @return
	 * @throws ConfigurationException
	 * @throws SlipStreamRuntimeException
	 */
	public abstract Representation doAction(Request request)
			throws SlipStreamRuntimeException, ConfigurationException;

	public static OneShotAction load(String uuid) {
		EntityManager em = PersistenceUtil.createEntityManager();
		return em.find(OneShotAction.class, uuid);
	}

	public void update() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		em.merge(this);
		transaction.commit();
	}

	public void store() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		em.persist(this);
		em.flush();
		transaction.commit();
	}

}
