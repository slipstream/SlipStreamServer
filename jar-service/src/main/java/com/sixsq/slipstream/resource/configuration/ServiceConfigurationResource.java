package com.sixsq.slipstream.resource.configuration;

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

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.authz.SuperEnroler;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotImplementedException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.resource.ParameterizedResource;
import com.sixsq.slipstream.util.ConfigurationUtil;
import com.sixsq.slipstream.util.RequestUtil;

public class ServiceConfigurationResource extends
		ParameterizedResource<ServiceConfiguration> {

	public static final String CONFIGURATION_PATH = "/configuration";

	private Configuration configuration;

	@Put
	public void update(Representation entity) {

		try {
			processEntityAsForm(entity);
		} catch (ConfigurationException e) {
			Util.throwConfigurationException(e);
		} catch (ValidationException e) {
			throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getMessage()));
		}

		ConnectorFactory.resetConnectors();

		String absolutePath = RequestUtil.constructAbsolutePath(CONFIGURATION_PATH);
		getResponse().redirectSeeOther(absolutePath);
	}

	public void processEntityAsForm(Representation entity)
			throws ResourceException, ConfigurationException,
			ValidationException {

		Form form = extractFormFromEntity(entity);
		ServiceConfigurationFormProcessor processor = new ServiceConfigurationFormProcessor(
				getUser());

		try {
			processor.processForm(form);
		} catch (BadlyFormedElementException e) {
			throwClientError(e);
		} catch (SlipStreamClientException e) {
			throwClientError(e);
		}

		ServiceConfiguration proposedServiceConfiguration = processor
				.getParametrized();
		configuration.update(proposedServiceConfiguration.getParameters());
		configuration.store();
	}

	@Post
	public void reloadConfigFile(Representation entity)
			throws ValidationException {

		try {
			configuration.reset();
		} catch (ConfigurationException e) {
			Util.throwConfigurationException(e);
		}
		configuration.store();
		ConnectorFactory.resetConnectors();

		String absolutePath = RequestUtil.constructAbsolutePath(getRequest().getResourceRef().getPath());
		getResponse().setLocationRef(absolutePath);
	}

	@Override
	protected String getPageRepresentation() {
		return "configuration";
	}

	@Override
	protected void setIsEdit() {
	}

	@Override
	protected void loadTargetParameterized() throws ValidationException {
		configuration = ConfigurationUtil.getConfigurationFromRequest(getRequest());
		setParameterized(configuration.getParameters());
	}

	@Override
	protected String extractTargetUriFromRequest() {
		return null;
	}

	@Override
	protected ServiceConfiguration getOrCreateParameterized(String name)
			throws ValidationException {
		throw (new NotImplementedException());
	}

	@Override
	protected void authorize() {

		if (!getClientInfo().getRoles().contains(SuperEnroler.Super)) {
			throw (new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					"Only administrators can access this resource"));
		}

		setCanPut(true);
		setCanGet(true);
	}

	@Override
	protected ServiceConfiguration loadParameterized(
			String targetParameterizedUri) throws ValidationException {
		throw (new NotImplementedException());
	}

}
