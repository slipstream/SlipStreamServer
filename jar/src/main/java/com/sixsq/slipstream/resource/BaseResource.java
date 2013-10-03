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

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.RequestUtil;

public abstract class BaseResource extends ServerResource {

	private User user = null;

	@Override
	protected void doInit() throws ResourceException {

		setUser(RequestUtil.getUserFromRequest(getRequest()));

	}

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

	protected void throwNotFoundResource() {
		throwClientError(Status.CLIENT_ERROR_NOT_FOUND, "Not found");
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
}
