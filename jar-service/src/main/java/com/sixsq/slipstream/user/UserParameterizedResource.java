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

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.ModuleUriUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.ResourceUriUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import javax.persistence.EntityManager;

public abstract class UserParameterizedResource extends BaseResource {

	private User parameterized = null;

	private String targetParameterizeUri = null;

	private boolean canGet = false;
	private boolean canPut = false;
	private boolean canPost = false;
	private boolean canDelete = false;

	@Override
	public void initialize() throws ResourceException {
		try {
			loadTargetParameterized();
		} catch (ValidationException e) {
			throwClientError(e);
		}

		try {
			setIsEdit();
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}
	}

	abstract protected String extractTargetUriFromRequest();

	abstract protected User getOrCreateParameterized(String name) throws ValidationException;

	public String getTargetParameterizeUri() {
		return targetParameterizeUri;
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

	public void setParameterized(User parameterized) {
		this.parameterized = parameterized;
	}

	protected EntityManager getEntityManager() {
		return (EntityManager) getRequest().getAttributes().get(ResourceUriUtil.ENTITY_MANAGER_KEY);
	}

	public User getParameterized() {
		return parameterized;
	}

	protected void loadTargetParameterized() throws ValidationException {

		targetParameterizeUri = extractTargetUriFromRequest();

		if (NEW_NAME.equals(ModuleUriUtil.extractShortNameFromResourceUri(targetParameterizeUri))) {
			createVolatileParameterizedForEditing();
		} else {
			setParameterized(loadParameterized(targetParameterizeUri));
		}

		if (getParameterized() == null) {
			setExisting(false);
		} else {
			Cookie cookie = CookieUtils.extractAuthnCookie(getRequest());
			String roles = CookieUtils.getCookieRoles(cookie);
			parameterized.setRoles(roles);
		}
	}

	abstract protected User loadParameterized(String targetParameterizedUri) throws ValidationException;

	private void createVolatileParameterizedForEditing() throws ValidationException {
		setParameterized(getOrCreateParameterized(ModuleUriUtil.extractModuleNameFromResourceUri(targetParameterizeUri)));
		setIsEdit(true);
	}

	@Override
	protected void setIsEdit() throws ConfigurationException, ValidationException {
		setIsEdit(isEdit() || isEditFlagTrue() || newTemplateResource());
	}

	protected boolean newInQuery() {
		return extractNewFlagFromQuery();
	}

	protected boolean newTemplateResource() {
		return isExisting()
				&& NEW_NAME.equals(ModuleUriUtil.extractShortNameFromResourceUri(getParameterized().getName()));
	}

	/**
	 * User requested the creation of a new resource. This means that if the
	 * resource already exists, for example, creation should be forbidden.
	 */
	protected boolean xxrequestingNew() {
		boolean newInUri = isExisting()
				&& NEW_NAME.equals(ModuleUriUtil.extractShortNameFromResourceUri(getParameterized().getName()));
		boolean newInQuery = extractNewFlagFromQuery();
		return newInQuery || newInUri;
	}

	protected void addParametersForEditing() throws ValidationException, ConfigurationException {
	}

	@Delete
	public void deleteResource() {

		if (!canDelete()) {
			throwClientForbiddenError();
		}

		getParameterized().remove();
		setStatus(Status.SUCCESS_NO_CONTENT);
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

		User prepared = null;
		try {
			prepared = prepareForSerialization();
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		}

		String result = SerializationUtil.toXmlString(prepared);
		return new StringRepresentation(result, MediaType.APPLICATION_XML);
	}

	@Get("html")
	public Representation toHtml() {
		checkCanGet();

		if (isEdit()) {
			try {
				addParametersForEditing();
			} catch (ValidationException e) {
				throwClientValidationError(e.getMessage());
			} catch (ConfigurationException e) {
				throwConfigurationException(e);
			}
		}

		User prepared = null;
		try {
			prepared = prepareForSerialization();
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		}

		String html = HtmlUtil.toHtml(prepared, getPageRepresentation(), getUser(), getRequest());

		return new StringRepresentation(html, MediaType.TEXT_HTML);
	}

	protected User prepareForSerialization() throws ConfigurationException, ValidationException {
		return getParameterized();
	}

	protected String getTransformationType() {
		String type = "view";
		if (isEdit()) {
			type = "edit";
		}
		if (newTemplateResource()) {
			type = "new";
		}
		if (isChooser()) {
			type = "chooser";
		}
		return type;
	}

	protected String getPageRepresentation() {
		return "unknown";
	}

	protected void checkCanGet() {
		if (!canGet()) {
			throwClientForbiddenError("Not allowed to access: " + targetParameterizeUri);
		}
	}

	protected void checkCanPut() {
		if (canPut()) {
			if (newInQuery() && isExisting()) {
				throwClientForbiddenError("Cannot create this resource. It already exists.");
			}
		} else {
			throwClientForbiddenError("Forbidden to update this resource.");
		}
	}

	protected boolean isChooser() {
		return false;
	}

	protected void setResponseCreatedAndViewLocation(String resourceUri) {
		getResponse().setStatus(Status.SUCCESS_CREATED);

		String redirectUrl = "/" + resourceUri;
		String absolutePath = RequestUtil.constructAbsolutePath(getRequest(), redirectUrl);

		getResponse().setLocationRef(absolutePath);
	}

	protected void setResponseOkAndViewLocation(String resourceUri) {
		Status status = isExisting() ? Status.SUCCESS_OK : Status.SUCCESS_CREATED;
		getResponse().setStatus(status);

		String redirectUrl = "/" + resourceUri;
		String absolutePath = RequestUtil.constructAbsolutePath(getRequest(), redirectUrl);

		getResponse().setLocationRef(absolutePath);
	}

	protected void setResponseRedirect(String resourceUri) {
		getResponse().redirectSeeOther(resourceUri);
	}

}