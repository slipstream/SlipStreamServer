package com.sixsq.slipstream.authn;

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

import static org.restlet.data.MediaType.APPLICATION_XHTML;
import static org.restlet.data.MediaType.TEXT_HTML;
import static org.restlet.data.Status.CLIENT_ERROR_UNAUTHORIZED;
import static org.restlet.data.Status.SUCCESS_OK;

import java.util.List;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.RequestUtil;

public class LoginResource extends AuthnResource {

	private static final String resourceRoot = "/login";

	public LoginResource() {
		super("login");
	}

	@Override
	protected void doInit() throws ResourceException {

		try {
			setUser(RequestUtil.getUserFromRequest(getRequest()));
		} catch (NullPointerException ex) {
			// user not logged-in. But it's ok for this page
		}

	}

	@Post
	public void login(Representation entity)
			throws ResourceException {

		Form form = new Form(entity);

		String username = form.getFirstValue("username");
		String password = form.getFirstValue("password");

		validateUser(username, password);

		setResponse(username);
	}

	private void validateUser(String username, String password) {

		if (username == null || password == null) {
			throwUnauthorizedWithMessage();
		}

		User dbUser = User.loadByName(username);

		if (dbUser == null) {
			throwUnauthorizedWithMessage();
		}

		String realPassword = dbUser.getPassword();
		if (!password.equals(realPassword)) {
			throwUnauthorizedWithMessage();
		}
	}

	private void throwUnauthorizedWithMessage() {
		throw new ResourceException(CLIENT_ERROR_UNAUTHORIZED, "Username/password combination not valid");
	}

	private void setResponse(String username) {
		Request request = getRequest();
		Response response = getResponse();
	
		CookieUtils.addAuthnCookie(response, username);
	
		if (isHtmlRequested(request)) {
			Reference redirectURL = extractRedirectURL(request);
			response.redirectSeeOther(redirectURL);
		} else {
			response.setStatus(SUCCESS_OK);
		}
	}

	private boolean isHtmlRequested(Request request) {

		ClientInfo clientInfo = request.getClientInfo();
		List<Preference<MediaType>> preferences = clientInfo
				.getAcceptedMediaTypes();

		for (Preference<MediaType> preference : preferences) {
			if (isHtmlLike(preference.getMetadata())) {
				return true;
			}
		}
		return false;
	}

	private boolean isHtmlLike(MediaType mediaType) {
		if (TEXT_HTML.isCompatible(mediaType)
				|| APPLICATION_XHTML.isCompatible(mediaType)) {
			return true;
		}
		return false;
	}

	public static String getResourceRoot() {
		return resourceRoot;
	}
	
}
