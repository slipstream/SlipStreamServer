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

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.util.RequestUtil;

/**
 * This resource will remove any authentication cookies from the client's cache.
 * It displays a confirmation form in response to a GET request. That form will
 * POST to the same URL with the yes/no answer from the user. The cookie is only
 * removed if the user confirms the logout.
 * 
 * @author loomis
 * 
 */
public class LogoutResource extends AuthnResource {

	public LogoutResource() {
		super("logout.xml");
	}

	@Post
	public void accept(Representation entity) {

		// Get the request and response.
		Request request = getRequest();
		Response response = getResponse();

		// Parse the form to extract the confirmation value.
		Form form = new Form(entity);
		String value = form.getFirstValue("confirm");

		// This is the redirection URL. By default, it will send the
		// user back to the welcome page of the application (i.e. baseUrl).
		Reference redirectRef = RequestUtil.getBaseRefSlash(request);

		// If the logout request was confirmed, remove the cookie. The baseUrl
		// (already set) is the correct place to go.
		if ("yes".equalsIgnoreCase(value)) {
			CookieUtils.removeAuthnCookie(response);
		} else {
			// Send the user back to the referencing page, if it was given in
			// the URL.
			redirectRef = extractRedirectURL(request);
		}

		// Always redirect the user to the welcome page or the referring page.
		response.redirectSeeOther(redirectRef);
	}

}
