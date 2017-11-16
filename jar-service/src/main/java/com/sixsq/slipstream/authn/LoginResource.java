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

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.RequestUtil;
import org.restlet.resource.ResourceException;

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

	public static String getResourceRoot() {
		return resourceRoot;
	}

}
