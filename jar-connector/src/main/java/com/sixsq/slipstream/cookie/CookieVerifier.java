package com.sixsq.slipstream.cookie;

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

import java.util.Arrays;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Cookie;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.security.Verifier;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;

public class CookieVerifier implements Verifier {

	@Override
	public int verify(Request request, Response response) {

		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		int result = RESULT_INVALID;
		
		if (cookie != null) {
			result = CookieUtils.verifyAuthnCookie(cookie);
			if (result == Verifier.RESULT_INVALID) {
				try {
					return checkUsernamePassword(request, response);
				} catch (ConfigurationException e) {
					throwConfigurationError(e);
				} catch (ValidationException e) {
					throwValidationError(e);
				}
			} else {
				return result;
			}
		} else {
			try {
				return checkUsernamePassword(request, response);
			} catch (ConfigurationException e) {
				throwConfigurationError(e);
			} catch (ValidationException e) {
				throwValidationError(e);
			}
		}

		return result;
	}

	private void throwValidationError(ValidationException e) {
		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
				"Validation error: " + e.getMessage());
	}

	private void throwConfigurationError(ConfigurationException e) {
		throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
				e.getMessage());
	}

	private int checkUsernamePassword(Request request, Response response)
			throws ConfigurationException, ValidationException {

		ChallengeResponse challengeResponse = request.getChallengeResponse();

		if (challengeResponse == null) {
			return Verifier.RESULT_MISSING;
		}

		String identifier = challengeResponse.getIdentifier();
		char[] secret = challengeResponse.getSecret();

		if (checkCredentialsInDb(identifier, secret)) {

			// Need to add cookie to request as well as response.
			CookieUtils.addAuthnCookie(response, "BASIC", identifier);

			return Verifier.RESULT_VALID;
		} else {
			return Verifier.RESULT_INVALID;
		}

	}

	private boolean checkCredentialsInDb(String identifier, char[] secret)
			throws ConfigurationException, ValidationException {

		User user = User.loadByName(identifier);
		if (user != null) {
			char[] correctPassword = user.getPassword().toCharArray();
			return Arrays.equals(secret, correctPassword);
		} else {
			return false;
		}

	}
}
