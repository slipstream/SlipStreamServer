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
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.ResourceUriUtil;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.security.User;
import org.restlet.security.Verifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class CookieAuthenticator extends AuthenticatorBase {

	private static final Logger logger = Logger.getLogger(CookieAuthenticator.class.getName());

	public CookieAuthenticator(Context context) {
		super(context, false);
	}

	@Override
	protected boolean authenticate(Request request, Response response) {

		Cookie cookie = CookieUtils.extractAuthnCookie(request);

		logger.fine("will authenticate cookie " + cookie);
		boolean isTokenValid = false;

		if(CookieUtils.isMachine(cookie)) {
			isTokenValid = CookieUtils.verifyAuthnCookie(cookie) == Verifier.RESULT_VALID;
			logger.fine("Done calling verifyAuthnCookie: isTokenValid=" + isTokenValid);
		} else {
			logger.fine("Will call claimsInToken");
			Map<String, String> claimsInToken = CookieUtils.claimsInToken(cookie);
			isTokenValid = !claimsInToken.isEmpty();
		}

		if (isTokenValid) {
			handleValid(request, cookie);
		} else {
			handleNotValid(request, response);
		}

		return isTokenValid;
	}

	private boolean handleValid(Request request, Cookie cookie) {

		String username = setClientInfo(request, cookie);

		com.sixsq.slipstream.persistence.User user = null;

		try {
			user = com.sixsq.slipstream.persistence.User.loadByName(username);
		} catch (ConfigurationException e) {
			return false;
		} catch (ValidationException e) {
			return false;
		}

		if(user == null) {
			return false;
		}

		setCloudServiceName(request, cookie);
		setUserInRequest(user, request);

		logger.fine("handle valid, cookie = " + cookie);
		String tokenInCookie = CookieUtils.tokenInCookie(cookie);
		if(tokenInCookie!=null) {
			logger.fine("handle valid, tokenInCookie = " + tokenInCookie);
			user.setAuthnToken(tokenInCookie);
			user = user.store();
			logger.fine("user.authnToken = " + user.getAuthnToken());
		}

		if (!CookieUtils.isMachine(cookie)) {
			setLastOnline(cookie);
		}

		return true;
	}

	private void handleNotValid(Request request, Response response) {
		CookieUtils.removeAuthnCookie(response);

		List<MediaType> supported = new ArrayList<MediaType>();
		supported.add(MediaType.APPLICATION_XML);
		supported.add(MediaType.TEXT_HTML);
		MediaType prefered = request.getClientInfo().getPreferredMediaType(supported);

		if (prefered != null && prefered.isCompatible(MediaType.TEXT_HTML)) {
			Reference baseRef = ResourceUriUtil.getBaseRef(request);

			Reference redirectRef = new Reference(baseRef, LoginResource.getResourceRoot());
			redirectRef.setQuery("redirectURL=" + request.getResourceRef().getPath());

			String absolutePath = RequestUtil.constructAbsolutePath(request, redirectRef.toString());

			response.redirectTemporary(absolutePath);
		} else {
			response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
		}
	}

	private String setClientInfo(Request request, Cookie cookie) {
		request.getClientInfo().setAuthenticated(true);

		String username = CookieUtils.getCookieUsername(cookie);
		logger.info("setClientInfo, username = '" + username + "'");

		User user = new User(username);
		request.getClientInfo().setUser(user);
		return username;
	}

	private void setCloudServiceName(Request request, Cookie cookie) {
		String cookieCloudServiceName = CookieUtils.getCookieCloudServiceName(cookie);
		if (cookieCloudServiceName != null) {
			request.getAttributes().put(RuntimeParameter.CLOUD_SERVICE_NAME, cookieCloudServiceName);
		}
	}

}
