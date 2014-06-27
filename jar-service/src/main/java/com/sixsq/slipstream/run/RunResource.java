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

import javax.persistence.EntityManager;

import org.restlet.Request;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.CannotAdvanceFromTerminalStateException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidStateException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.ResourceUriUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.Terminator;

public class RunResource extends BaseResource {

	private Run run = null;

	@Override
	public void initialize() throws ResourceException {

		Request request = getRequest();

		validateUser();

		String resourceUri = ResourceUriUtil.extractResourceUri(request);
		run = Run.load(resourceUri);

		if (run == null) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}

	}

	@Override
	protected void authorize() {

		if (getUser().isSuper()) {
			return;
		}
		if (!getUser().getName().equals(run.getUser())) {
			throw (new ResourceException(Status.CLIENT_ERROR_FORBIDDEN));
		}

	}

	@Get("xml")
	public Representation toXml() throws NotFoundException,
			ValidationException, ConfigurationException {

		EntityManager em = PersistenceUtil.createEntityManager();
		Run run;
		String xml;
		try {
			run = constructRun(em, true);
			xml = SerializationUtil.toXmlString(run);
		} catch (SlipStreamClientException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		} finally {
			em.close();
		}

		return new StringRepresentation(xml, MediaType.APPLICATION_XML);
	}

	@Get("html")
	public Representation toHtml() throws ConfigurationException,
			 ValidationException {

		EntityManager em = PersistenceUtil.createEntityManager();
		String html;
		try {
			Run run = constructRun(em, false);
			html = HtmlUtil.toHtml(run, getPageRepresentation(), getUser());
		} catch (SlipStreamClientException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		} finally {
			em.close();
		}

		return new StringRepresentation(html, MediaType.TEXT_HTML);

	}

	protected String getPageRepresentation() {
		return "run";
	}

	private Run constructRun(EntityManager em, boolean withVmState)
			throws SlipStreamClientException {

		Run run = Run.load(this.run.getResourceUri(), em);
		User user = User.loadByName(run.getUser());

		if (withVmState) {
			try {
				run = updateVmStatus(run, user);
			} catch (SlipStreamClientException e) {
				run = Run.abortOrReset(e.getMessage(), "", em, run.getUuid());
			} catch (SlipStreamException e) {
				getLogger().warning(
						"Error updating vm status for run " + run.getName()
								+ ": " + e.getMessage());
			}
		}

		Module module = RunFactory.selectFactory(run.getType()).overloadModule(run, user);
		run.setModule(module, true);

		return run;
	}

	private Run updateVmStatus(Run run, User user) throws SlipStreamException {
		return RunFactory.updateVmStatus(run, user);
	}

	private void validateUser() {
		// FIXME: This should either do something or be moved to guard.
	}

	@Delete
	public void terminate() {

		try {

			Terminator.terminate(this.run.getResourceUri());

		} catch (CannotAdvanceFromTerminalStateException e) {
		} catch (InvalidStateException e){
			throwClientConflicError(e.getMessage());
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

	}
}
