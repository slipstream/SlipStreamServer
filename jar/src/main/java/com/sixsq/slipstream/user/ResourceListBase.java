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

import org.restlet.Request;
import org.restlet.data.Cookie;
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
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.persistence.Parameterized;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public abstract class ResourceListBase<S extends Parameterized<S, ?>> extends
		ServerResource {

	private S parameterized = null;

	private User user = null;

	private boolean isEdit = false;

	private boolean canDelete = false;
	private boolean canPut = false;
	private boolean canPost = false;
	private boolean canCreateChildren = false;

	protected Configuration configuration;

	public ResourceListBase() {
		super();
	}

	@Override
	public void doInit() throws ResourceException {

		Request request = getRequest();

		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		setUser(CookieUtils.getCookieUser(cookie));

		configuration = RequestUtil.getConfigurationFromRequest(request);

		loadTargetParameterized();

		authorize();

		setIsEdit();
	}

	abstract protected String extractUrlToken();

	abstract protected S createParameterized(String name);
	
	public boolean isEdit() {
		return isEdit;
	}

	public void setEdit(boolean isEdit) {
		this.isEdit = isEdit;
	}

	public boolean canDelete() {
		return canDelete;
	}

	public void setCanDelete(boolean canDelete) {
		this.canDelete = canDelete;
	}

	public boolean isCanPut() {
		return canPut;
	}

	public void setCanPut(boolean canPut) {
		this.canPut = canPut;
	}

	public boolean isCanPost() {
		return canPost;
	}

	public void setCanPost(boolean canPost) {
		this.canPost = canPost;
	}

	public boolean isCanCreateChildren() {
		return canCreateChildren;
	}

	public void setCanCreateChildren(boolean canCreateChildren) {
		this.canCreateChildren = canCreateChildren;
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

	public S getParameterized() {
		return parameterized;
	}

	protected void loadTargetParameterized() {
	
		String targetParameterizedName = extractUrlToken();
	
		if (User.NEW_NAME.equals(targetParameterizedName)) {
			setEditableTemplate();
		} else {
			setParameterized(loadParameterized(createParameterized(targetParameterizedName).getResourceUri()));
		}
	
		if (getParameterized() == null) {
			setExisting(false);
		}
	}
	
	abstract protected S loadParameterized(String resourceUri);

	private void setEditableTemplate() {
		setParameterized(createParameterized(User.NEW_NAME));
		isEdit = true;
	}

	protected boolean isNew() {
		return isExisting() && User.NEW_NAME.equals(getParameterized().getName());
	}

	protected void setIsEdit() {
		isEdit = isEdit || extractEditFlagFromQuery() || isNew();
	}

	private boolean extractEditFlagFromQuery() {
		Reference resourceRef = getRequest().getResourceRef();
		Form form = resourceRef.getQueryAsForm();
		String flag = form.getFirstValue("edit");
		return ("true".equalsIgnoreCase(flag) || "yes".equalsIgnoreCase(flag) || "on"
				.equalsIgnoreCase(flag));
	}

	abstract protected String getViewStylesheet();

	abstract protected String getEditStylesheet();

	protected void addParametersForEditing() {
	}

	protected void throwUnauthorized() {
		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
				"You are not allowed to access this resource");
	}

	protected void throwClientError(Throwable e) {
		throwClientError(e.getMessage());
	}

	protected void throwClientError(String message) {
		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, message);
	}

	@Delete
	public Representation deleteUser() {

		if(!canDelete()) {
			throwForbidden();
		}
		
		getParameterized().remove();
		setStatus(Status.SUCCESS_NO_CONTENT);

		return null;
	}

	private void throwForbidden() {
		throw(new ResourceException(Status.CLIENT_ERROR_FORBIDDEN));
	}

	@Get("txt")
	public Representation toTxt() {
		String result = SerializationUtil.toXmlString(getParameterized());
		return new StringRepresentation(result);
	}

	@Get("xml")
	public Representation toXml() {
		String result = SerializationUtil.toXmlString(getParameterized());
		return new StringRepresentation(result);
	}

	@Get("html")
	public Representation toHtml() {
	
		Request request = getRequest();
		String baseUrlSlash = RequestUtil.getBaseUrlSlash(request);
	
		String stylesheet = prepareTransformation();
	
		return HtmlUtil.transformToHtml(baseUrlSlash,
				getParameterized().getResourceUri(), configuration.version, stylesheet,
				getUser(), getParameterized());
	
	}

	private String prepareTransformation() {
	
		String stylesheet;
		if (isEdit) {
			addParametersForEditing();
			stylesheet = getEditStylesheet();
		} else {
			stylesheet = getViewStylesheet();
		}
	
		return stylesheet;
	}

}