package com.sixsq.slipstream.resource;

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

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Empty;
import com.sixsq.slipstream.persistence.Metadata;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.ConfigurationUtil;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public abstract class BaseResource extends ServerResource {

	private User user = null;
	private ServiceConfiguration configuration = null;
	protected static final String NEW_NAME = "new";
	private boolean isEdit = false;

	@Override
	protected void doInit() throws ResourceException {
		try {
			setUser(RequestUtil.getUserFromRequest(getRequest()));
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}
		configuration = ConfigurationUtil
				.getServiceConfigurationFromRequest(getRequest());
	}

	@Get("xml|txt")
	public Representation toXml() throws NotFoundException,
			ValidationException, ConfigurationException {

		String result = "";
		User user = getUser();
		if (user != null) {
			result = SerializationUtil.toXmlString(getUser());
		}
		return new StringRepresentation(result);
	}

	@Get("html")
	public Representation toHtml() throws ConfigurationException,
			ValidationException {

		Metadata metadata;
		User user = getUser();
		if (user == null) {
			metadata = new Empty();
		} else {
			metadata = user;
		}
		return new StringRepresentation(HtmlUtil.toHtml(metadata,
				getPageRepresentation(), getTransformationType(), getUser()),
				MediaType.TEXT_HTML);

	}

	protected abstract String getPageRepresentation();

	protected void setUser(User user) {
		this.user = user;
	}

	protected User getUser() {
		return user;
	}

	protected String getTransformationType() {
		String type = "view";
		if (isChooser()) {
			type = "chooser";
		}
		return type;
	}

	protected boolean isChooser() {
		String c = (String) getRequest().getAttributes().get("chooser");
		return (c == null) ? false : true;
	}

	public ServiceConfiguration getConfiguration() {
		return configuration;
	}

	protected void throwUnauthorized() {
		Util.throwUnauthorized();
	}

	protected void throwClientError(Throwable e) {
		Util.throwClientError(e);
	}

	protected void throwClientError(String message) {
		Util.throwClientError(message);
	}

	protected void throwClientConflicError(String message) {
		Util.throwClientConflicError(message);
	}

	protected void throwClientForbiddenError() {
		Util.throwClientForbiddenError();
	}

	protected void throwClientForbiddenError(String message) {
		Util.throwClientForbiddenError(message);
	}

	protected void throwClientForbiddenError(Throwable e) {
		Util.throwClientForbiddenError(e);
	}

	protected void throwClientBadRequest(String message) {
		Util.throwClientBadRequest(message);
	}

	protected void throwNotFoundResource() {
		Util.throwNotFoundResource();
	}

	protected void throwClientValidationError(String message) {
		Util.throwClientValidationError(message);
	}

	protected void throwClientConflicError(Throwable e) {
		Util.throwClientConflicError(e);
	}

	protected void throwClientError(Status status, String message) {
		Util.throwClientError(status, message);
	}

	protected void throwClientError(Status status, Throwable e) {
		Util.throwClientError(status, e);
	}

	protected void throwConfigurationException(ConfigurationException e) {
		Util.throwConfigurationException(e);
	}

	protected void throwServerError(Throwable e) {
		Util.throwServerError(e);
	}

	protected void throwServerError(String message) {
		Util.throwServerError(message);
	}

	protected void setIsEdit() throws ConfigurationException,
			ValidationException {
		isEdit = isEdit || isEditFlagTrue();
	}

	protected void setIsEdit(boolean isEdit) throws ConfigurationException,
			ValidationException {
		this.isEdit = isEdit;
	}

	protected boolean isEdit() {
		return isEdit;
	}

	protected boolean isEditFlagTrue() {
		return isSetInQuery("edit");
	}

	private boolean isQueryValueSetTrue(String flag) {
		String value = getQueryValue(flag);
		return isTrue(value);
	}

	protected boolean isTrue(String value) {
		if(value == null) {
			return false;
		}
		String trimmed = value.trim().toLowerCase();
		return ("true".equals(trimmed) || "yes".equals(trimmed) || "on"
				.equals(trimmed));
	}

	private boolean isSetInQuery(String key) {
		Reference resourceRef = getRequest().getResourceRef();
		Form form = resourceRef.getQueryAsForm();
		return isTrue(form.getFirstValue(key));
	}

	protected boolean extractNewFlagFromQuery() {
		return isQueryValueSetTrue("new");
	}

	protected Form extractFormFromEntity(Representation entity)
			throws ResourceException {
			
				Form form = null;
				try {
					form = new Form(entity.getText());
				} catch (IOException e) {
					String msg = "Failed retreiving text from entity. "
							+ e.getMessage();
					throwClientError(msg);
				}
				return form;
			}

	protected void checkIsSuper() {
		if(!isSuper()) {
			throwClientForbiddenError("Only privileged users can perform this action");
		}
	}

	protected boolean isSuper() {
		return getUser().isSuper();
	}

	protected void setEmptyEntity(MediaType mt) {
		getResponse().setEntity(null, mt);
	}
}
