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

import java.util.HashSet;

import javax.persistence.EntityManager;

import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class RunResource extends BaseResource {

	private Run run = null;

	@Override
	public void doInit() throws ResourceException {

		super.doInit();

		Request request = getRequest();

		validateUser();

		String resourceUri = RequestUtil.extractResourceUri(request);
		run = Run.load(resourceUri);

		if (run == null) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}

		authorize();
	}

	private void authorize() {
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

		Run run;
		try {
			run = constructRun();
		} catch (SlipStreamClientException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					e.getMessage());
		}

		String result = SerializationUtil.toXmlString(run);

		return new StringRepresentation(result, MediaType.TEXT_XML);
	}

	@Get("html")
	public Representation toHtml() throws ConfigurationException,
			NotFoundException, ValidationException {

		Run run;
		try {
			run = constructRun();
		} catch (SlipStreamClientException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					e.getMessage());
		}

		String html = HtmlUtil.toHtml(run, getPageRepresentation(), getUser());

		return new StringRepresentation(html, MediaType.TEXT_HTML);

	}

	private String getPageRepresentation() {
		return "run";
	}

	private Run constructRun() throws SlipStreamClientException {

		Run run = Run.load(this.run.getResourceUri());

		Module module = RunFactory.selectFactory(run.getType()).overloadModule(
				run, getUser());
		run.setModule(module, true);

		return run;
	}

	private void validateUser() {
		// FIXME: This should either do something or be moved to guard.
	}

	@Put("form")
	public void update(Representation entity) {
		Form form = new Form(entity);
		String tags = form.getFirstValue(RuntimeParameter.TAGS_KEY, null);
		if (tags != null) {
			RuntimeParameter rtp = RuntimeParameter.loadFromUuidAndKey(
					run.getUuid(), RuntimeParameter.GLOBAL_TAGS_KEY);
			rtp.setValue(tags);
			rtp.store();
		}
	}

	@Delete
	public void terminate() {

		EntityManager em = PersistenceUtil.createEntityManager();

		Run run = Run.load(this.run.getResourceUri(), em);

		try {
			if (run.getCategory() == ModuleCategory.Deployment) {
				HashSet<String> cloudServicesList = run.getCloudServicesList();
				for (String cloudServiceName : cloudServicesList) {
					Connector connector = ConnectorFactory
							.getConnector(cloudServiceName);
					try {
						connector.terminate(run, getUser());
					} catch (SlipStreamException e) {
						throw new ResourceException(
								Status.CLIENT_ERROR_CONFLICT,
								"Failed terminating VMs", e);
					}
				}
			} else {
				Connector connector = ConnectorFactory.getConnector(run
						.getCloudService());
				try {
					connector.terminate(run, getUser());
				} catch (SlipStreamException e) {
					throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
							"Failed terminating VMs", e);
				}
			}
		} catch (ConfigurationException e) {
			e.printStackTrace();
			throw (new ResourceException(Status.SERVER_ERROR_INTERNAL, e));
		} catch (ValidationException e) {
			throw (new ResourceException(Status.CLIENT_ERROR_CONFLICT, e));
		}

		run.done();

		run.store();

		em.close();
	}
}
