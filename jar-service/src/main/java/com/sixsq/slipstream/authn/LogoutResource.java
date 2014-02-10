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

import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.util.RequestUtil;

/**
 * The DELETE action on this resource will remove any authentication cookies from 
 * the client's cache.
 * The confirmation of deletion is expected to take place in the browser.
 * The user is redirected to the login page by default, or the redirect
 * query parameter if present.
 */
public class LogoutResource extends AuthnResource {

	@Override
	protected void doInit() throws ResourceException {

	}

	public LogoutResource() {
		super("logout");
	}

	@Delete
	public void removeCookie(Representation entity) {
        CookieUtils.removeAuthnCookie(getResponse());
		Reference redirectURL = extractRedirectURL(getRequest(), "login");
		String absolutePath = RequestUtil.constructAbsolutePath(redirectURL.getPath());
		getResponse().redirectSeeOther(absolutePath);
	}

}
