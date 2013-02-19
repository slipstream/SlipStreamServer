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

import javax.persistence.EntityManager;

import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.ParametersFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.Parameterized;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.ModuleUriUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public abstract class ParameterizedResource<S extends Parameterized<S, ?>>
		extends ServerResource {

	protected static final String NEW_NAME = "new";

	private S parameterized = null;

	private User user = null;

	protected String baseUrlSlash = null;
	protected String targetParameterizeUri = null;

	private boolean isEdit = false;

	private boolean canGet = false;
	private boolean canPut = false;
	private boolean canPost = false;
	private boolean canDelete = false;

	protected Configuration configuration;

	@Override
	public void doInit() throws ResourceException {

		Request request = getRequest();

		setUser(User.loadByName(request.getClientInfo().getUser().getName()));

		configuration = RequestUtil.getConfigurationFromRequest(request);

		baseUrlSlash = RequestUtil.getBaseUrlSlash(request);

		try {
			loadTargetParameterized();
		} catch (ValidationException e) {
			throwClientError(e);
		}

		authorize();

		setIsEdit();
	}

	abstract protected String extractTargetUriFromRequest();

	abstract protected S createParameterized(String name)
			throws ValidationException;

	public boolean isEdit() {
		return isEdit;
	}

	public void setEdit(boolean isEdit) {
		this.isEdit = isEdit;
	}

	public boolean canGet() {
		return canGet;
	}

	public void setCanGet(boolean canGet) {
		this.canGet = canGet;
	}

	public boolean canDelete() {
		return canDelete;
	}

	public void setCanDelete(boolean canDelete) {
		this.canDelete = canDelete;
	}

	public boolean canPut() {
		return canPut;
	}

	public void setCanPut(boolean canPut) {
		this.canPut = canPut;
	}

	public boolean canPost() {
		return canPost;
	}

	public void setCanPost(boolean canPost) {
		this.canPost = canPost;
	}

	abstract protected void authorize();

	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public void setParameterized(S parameterized) {
		this.parameterized = parameterized;
	}

	protected EntityManager getEntityManager() {
		return (EntityManager) getRequest().getAttributes().get(
				RequestUtil.ENTITY_MANAGER_KEY);
	}

	public S getParameterized() {
		return parameterized;
	}

	protected void loadTargetParameterized() throws ValidationException {

		targetParameterizeUri = extractTargetUriFromRequest();

		if (NEW_NAME.equals(ModuleUriUtil
				.extractShortNameFromResourceUri(targetParameterizeUri))) {
			createVolatileParameterizedForEditing();
		} else {
			setParameterized(loadParameterized(targetParameterizeUri));
		}

		if (getParameterized() == null) {
			setExisting(false);
		}
	}

	abstract protected S loadParameterized(String targetParameterizedUri)
			throws ValidationException;

	private void createVolatileParameterizedForEditing() throws ValidationException {
		setParameterized(createParameterized(ModuleUriUtil
				.extractModuleNameFromResourceUri(targetParameterizeUri)));
		isEdit = true;
	}

	protected boolean isNew() {
		return isExisting()
				&& NEW_NAME.equals(ModuleUriUtil
						.extractShortNameFromResourceUri(getParameterized()
								.getName()));
	}

	protected void setIsEdit() {
		isEdit = isEdit || extractEditFlagFromQuery() || isNew();
	}

	private boolean extractEditFlagFromQuery() {
		Reference resourceRef = getRequest().getResourceRef();
		Form form = resourceRef.getQueryAsForm();
		String edit = form.getFirstValue("edit");
		return ("true".equalsIgnoreCase(edit) || "yes".equalsIgnoreCase(edit) || "on"
				.equalsIgnoreCase(edit));
	}

	abstract protected String getViewStylesheet();

	abstract protected String getEditStylesheet();

	protected void addParametersForEditing() throws ValidationException,
			ConfigurationException {
		user = ParametersFactory.addParametersForEditing(user);
	}

	protected void throwUnauthorized() {
		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
				"You are not allowed to access this resource");
	}

	protected void throwClientError(Throwable e) {
		throwClientError(e.getMessage());
	}

	protected void throwClientError(String message) {
		throwClientError(Status.CLIENT_ERROR_BAD_REQUEST, message);
	}

	protected void throwClientConflicError(String message) {
		throwClientError(Status.CLIENT_ERROR_CONFLICT, message);
	}

	protected void throwClientForbiddenError() {
		throwClientError(Status.CLIENT_ERROR_FORBIDDEN, "");
	}

	protected void throwClientForbiddenError(String message) {
		throwClientError(Status.CLIENT_ERROR_FORBIDDEN, message);
	}

	protected void throwClientForbiddenError(Throwable e) {
		throwClientError(Status.CLIENT_ERROR_FORBIDDEN, e);
	}

	protected void throwClientBadRequest(String message) {
		throwClientError(Status.CLIENT_ERROR_BAD_REQUEST, message);
	}

	protected void throwClientValidationError(String message) {
		throwClientError(Status.CLIENT_ERROR_BAD_REQUEST, "Validation error: "
				+ message);
	}

	protected void throwClientConflicError(Throwable e) {
		throwClientError(Status.CLIENT_ERROR_CONFLICT, e);
	}

	protected void throwClientError(Status status, String message) {
		throw new ResourceException(status, message);
	}

	protected void throwClientError(Status status, Throwable e) {
		throw new ResourceException(status, e);
	}

	protected void throwServerError(Throwable e) {
		throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
	}

	protected void throwServerError(String message) {
		throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message);
	}

	@Delete
	public void deleteResource() {

		if (!canDelete()) {
			throwForbidden();
		}

		getParameterized().remove();
		setStatus(Status.SUCCESS_NO_CONTENT);
	}

	private void throwForbidden() {
		throw (new ResourceException(Status.CLIENT_ERROR_FORBIDDEN));
	}

	@Get("txt")
	public Representation toTxt() {
		checkCanGet();

		String result = SerializationUtil.toXmlString(getParameterized());
		return new StringRepresentation(result);
	}

	@Get("xml")
	public Representation toXml() {
		checkCanGet();

		String result = SerializationUtil.toXmlString(getParameterized());
		return new StringRepresentation(result);
	}

	@Get("html")
	public Representation toHtml() {
		checkCanGet();

		Request request = getRequest();
		String baseUrlSlash = RequestUtil.getBaseUrlSlash(request);

		String stylesheet = null;
		try {
			stylesheet = prepareTransformation();
		} catch (ValidationException e) {
			throwClientError(e.getMessage());
		} catch (ConfigurationException e) {
			throwServerError(e);
		}

		return HtmlUtil.transformToHtml(baseUrlSlash, getParameterized()
				.getResourceUri(), configuration.version, stylesheet,
				getUser(), getParameterized(), getChooser());

	}

	protected void checkCanGet() {
		if (!canGet()) {
			throwClientForbiddenError("Not allowed to access: "
					+ targetParameterizeUri);
		}
	}

	protected void checkCanPut() {
		if (!canPut()) {
			if (isNew()) {
				throwClientForbiddenError("Cannot create this resource. Does it already exist?");
			} else {
				throwClientForbiddenError("Forbidden to update this resource.");
			}
		}
	}

	protected ModuleCategory getChooser() {
		return null;
	}

	private String prepareTransformation() throws ValidationException,
			ConfigurationException {

		String stylesheet;
		if (isEdit) {
			addParametersForEditing();
			stylesheet = getEditStylesheet();
		} else {
			stylesheet = getViewStylesheet();
		}

		return stylesheet;
	}

	protected void setResponseCreatedAndViewLocation(String resourceUri) {
		getResponse().setStatus(Status.SUCCESS_CREATED);
		String redirectUrl = getRequest().getRootRef() + "/" + resourceUri;
		getResponse().setLocationRef(redirectUrl);
	}

	protected void setResponseOkAndViewLocation(String resourceUri) {
		Status status = isExisting() ? Status.SUCCESS_OK : Status.SUCCESS_CREATED;
		getResponse().setStatus(status);
		String redirectUrl = getRequest().getRootRef() + "/" + resourceUri;
		getResponse().setLocationRef(redirectUrl);
	}

	protected void setResponseRedirect(String resourceUri) {
		getResponse().redirectSeeOther(resourceUri);
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

}