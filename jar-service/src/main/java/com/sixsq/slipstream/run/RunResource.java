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

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.exceptions.CannotAdvanceFromTerminalStateException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidStateException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.Terminator;

public class RunResource extends RunBaseResource {

	private Run run = null;

	@Override
	public void initializeSubResource() throws ResourceException {

		long start = System.currentTimeMillis();
		long before;

		before = System.currentTimeMillis();
		run = Run.loadFromUuid(getUuid());
		logTimeDiff("load", before);

		if (run == null) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}

		logTimeDiff("initialize on run", start);
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

		long start = System.currentTimeMillis();
		long before;

		EntityManager em = PersistenceUtil.createEntityManager();

		String xml;
		try {
			before = System.currentTimeMillis();
			Run run = constructRun(em);
			logTimeDiff("constructRun", before);
			before = System.currentTimeMillis();
			xml = SerializationUtil.toXmlString(run);
			logTimeDiff("xml serialisation", before);
		} catch (SlipStreamClientException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		} finally {
			em.close();
		}

		logTimeDiff("processing get on run", start);

		return new StringRepresentation(xml, MediaType.APPLICATION_XML);
	}

	@Get("html")
	public Representation toHtml() throws ConfigurationException,
			 ValidationException {

		long start = System.currentTimeMillis();
		long before;

		EntityManager em = PersistenceUtil.createEntityManager();
		String html;
		try {
			before = System.currentTimeMillis();
			Run run = constructRun(em);
			logTimeDiff("constructRun", before);
			before = System.currentTimeMillis();
			html = HtmlUtil.toHtml(run, getPageRepresentation(), getUser(), getRequest());
			logTimeDiff("html generation", before);
		} catch (SlipStreamClientException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		} finally {
			em.close();
		}

		logTimeDiff("processing get on run", start);

		return new StringRepresentation(html, MediaType.TEXT_HTML);

	}

	protected String getPageRepresentation() {
		return "run";
	}

	private Run constructRun(EntityManager em)
			throws SlipStreamClientException {

		Run run = Run.load(this.run.getResourceUri(), em);

		Module module = RunFactory.loadModule(run);
		run.setModule(module);

		return run;
	}

	@Delete
	public void terminate() {

		try {
			
			this.run.postEventTerminate();
			Terminator.terminate(this.run.getResourceUri());

		} catch (CannotAdvanceFromTerminalStateException e) {
		} catch (InvalidStateException e){
			throwClientConflicError(e.getMessage());
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

	}
}
