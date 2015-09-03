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
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.ResourceUriUtil;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.security.User;
import org.restlet.security.Verifier;

import java.util.ArrayList;
import java.util.List;

public class BasicAuthenticator extends AuthenticatorBase {

	public BasicAuthenticator(Context context) {
		super(context, false);
	}

	@Override
	protected boolean authenticate(Request request, Response response) {

		if (request.getClientInfo().isAuthenticated()) {
			return true;
		}

		int result = Verifier.RESULT_INVALID;

		if (request.getChallengeResponse() == null) {
			result = Verifier.RESULT_MISSING;

		} else {

			String username = request.getChallengeResponse().getIdentifier();
			String password = String.copyValueOf(request.getChallengeResponse().getSecret());
			com.sixsq.slipstream.persistence.User user = null;

			try {
				user = com.sixsq.slipstream.persistence.User.loadByName(username);
			} catch (ConfigurationException e) {
				Util.throwConfigurationException(e);
			} catch (ValidationException e) {
				Util.throwServerError(e.getMessage());
			}

			try {
				Response token = (new AuthProxy()).createToken(username, password);
				CookieUtils.addAuthnCookieFromAuthnResponse(response, token);

				// CookieUtils.addAuthnCookie(response, "local", username); TODO, local?

				setClientInfo(request, username);
				setUserInRequest(user, request);
				setLastOnline(user);

				return true;
			} catch (ResourceException re) {
				result = Verifier.RESULT_INVALID;
			}
		}

		redirectOrUnauthorized(request, response, result);
		return false;
	}

	private void redirectOrUnauthorized(Request request, Response response, int result) {

		if (result == Verifier.RESULT_INVALID) {
			CookieUtils.removeAuthnCookie(response);
		}

		List<MediaType> supported = new ArrayList<MediaType>();
		supported.add(MediaType.APPLICATION_XML);
		supported.add(MediaType.TEXT_HTML);
		MediaType prefered = request.getClientInfo().getPreferredMediaType(supported);

		if (prefered != null && prefered.isCompatible(MediaType.TEXT_HTML)) {
			Reference baseRef = ResourceUriUtil.getBaseRef(request);

			Reference redirectRef = new Reference(baseRef, LoginResource.getResourceRoot());
			redirectRef.setQuery("redirectURL=" + request.getResourceRef().getPath().toString());

			String absolutePath = RequestUtil.constructAbsolutePath(request, redirectRef.toString());

			response.redirectTemporary(absolutePath);
		} else {
			response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
		}
	}

	private void setClientInfo(Request request, String username) {
		request.getClientInfo().setAuthenticated(true);
		request.getClientInfo().setUser(new User(username));
	}

}
