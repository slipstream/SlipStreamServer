package com.sixsq.slipstream.user;

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
import java.util.Map.Entry;

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.ParametersFactory;
import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.resource.ParameterizedResource;

/**
 * @see UserResourceTest
 */
public class UserResource extends ParameterizedResource<User> {

	private static final String resourceRoot = User.RESOURCE_URL_PREFIX;

	@Get("txt")
	@Override
	public Representation toTxt() {
		return super.toTxt();
	}

	@Get("xml")
	public Representation toXml() {
		User user = (User)getParameterized();
		
		mergeCloudSystemParameters(user);		

		return super.toXml();
	}

	private void mergeCloudSystemParameters(User user) {
		String cloudServiceName = (String) getRequest().getAttributes().get(RuntimeParameter.CLOUD_SERVICE_NAME);
		if (cloudServiceName != null) {
			for (Entry<String, Parameter<ServiceConfiguration>> p : Configuration.getInstance().getParameters().getParameters(cloudServiceName).entrySet()) {
				user.getParameters().put(p.getKey(), UserParameter.convert(p.getValue()));
			}
		}
	}

	@Get("html")
	public Representation toHtml() {
		return super.toHtml();
	}

	@Override
	protected String extractTargetUriFromRequest() {
		return (String) getRequest().getAttributes().get("user");
	}

	@Override
	protected User createParameterized(String name) {
		User user = getParameterized();
		if (user == null) {
			try {
				user = new User(name);
			} catch (ValidationException e) {
				throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						e.getMessage()));
			}
		}
		return user;
	}

	@Override
	protected void authorize() {

		setCanPut(getUser().isSuper() || isNew() || isItSelf());
		setCanDelete(getUser().isSuper() || isItSelf());

		if (getUser().isSuper() || isNew() || isItSelf()) {
			setCanGet(true);
		}

	}

	private boolean isItSelf() {
		return isExisting()
				&& getUser().getResourceUri().equals(
						getTargetUser().getResourceUri());
	}

	private User getTargetUser() {
		return (User) getParameterized();
	}

	@Override
	protected String getViewStylesheet() {
		String stylesheet;
		stylesheet = "user.xsl";
		return stylesheet;
	}

	@Override
	protected String getEditStylesheet() {
		String stylesheet;
		stylesheet = "user-editor.xsl";
		return stylesheet;
	}

	@Override
	protected void addParametersForEditing() throws ValidationException,
			ConfigurationException {

		ParametersFactory.addParametersForEditing(getParameterized());
	}

	@Put("form")
	public void modifyOrCreateFromForm(Representation entity)
			throws ResourceException {

		setParameterized(createParameterized(targetParameterizeUri));

		try {
			ParametersFactory.addParametersForEditing(getParameterized());
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		} catch (ConfigurationException e) {
			throwServerError(e.getMessage());
		}

		processEntityAsForm(entity);

		try {
			updateOrCreate(getParameterized());
		} catch (ValidationException e) {
			throwClientBadRequest(e.getMessage());
		}
	}

	private void updateOrCreate(User user) throws ValidationException {

		checkCanPut();

		try {
			user.validate();
		} catch (ValidationException ex) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, ex);
		}

		if (!targetParameterizeUri.equals(user.getName())) {
			throwClientBadRequest("The uploaded user does not correspond to the target user uri");
		}

		user.store();

		setResponseForPut();
	}

	private void setResponseForPut() {
		if (isExisting()) {
			setResponseOkAndViewLocation(getParameterized().getResourceUri());
		} else {
			setResponseCreatedAndViewLocation(getParameterized()
					.getResourceUri());
		}
	}

	private void processEntityAsForm(Representation entity) {

		Form form = extractForm(entity);
		processForm(form);
	}

	private Form extractForm(Representation entity) {
		Form form = null;
		try {
			String text = entity.getText();
			form = new Form(text);
		} catch (IOException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"cannot extract text from entity");
		}
		return form;
	}

	private void processForm(Form form) {

		UserFormProcessor processor = new UserFormProcessor(getParameterized(),
				getUser());
		try {
			processor.processForm(form);
		} catch (BadlyFormedElementException e) {
			throwClientError(e);
		} catch (SlipStreamClientException e) {
			throwClientError(e);
		} catch (IllegalArgumentException e) {
			throwClientError(e);
		}

	}

	public static String getResourceRoot() {
		return resourceRoot;
	}

	@Override
	protected User loadParameterized(String targetParameterizedName) {
		return User.loadByName(targetParameterizedName);
	}

}
