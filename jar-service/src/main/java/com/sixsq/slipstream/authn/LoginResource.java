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

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.RequestUtil;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import java.util.List;

import static org.restlet.data.MediaType.APPLICATION_XHTML;
import static org.restlet.data.MediaType.TEXT_HTML;
import static org.restlet.data.Status.SUCCESS_OK;

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
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

	}

	@Post
	public void login(Representation entity) throws ResourceException {

		Form form = new Form(entity);

		String username = form.getFirstValue("username");
		String password = form.getFirstValue("password");

		try {
			validateUser(username, password);
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		setResponse(username);
	}

	private void validateUser(String username, String password) throws ConfigurationException, ValidationException {
		AuthProxy authProxy = new AuthProxy();
		authProxy.validateUser(username, password);
	}
	
	private void setResponse(String username) {
		Request request = getRequest();
		Response response = getResponse();

		CookieUtils.addAuthnCookie(response, username);

		if (isHtmlRequested(request)) {
			String redirectPath = extractRedirectURL(request).getRelativePart();
			String redirectURL = RequestUtil.constructAbsolutePath(request, redirectPath);
			response.redirectSeeOther(redirectURL);
		} else {
			response.setEntity(null, MediaType.ALL);
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
