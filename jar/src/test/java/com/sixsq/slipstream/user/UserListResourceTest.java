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


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.ResourceTestBase;

public class UserListResourceTest extends ResourceTestBase {

	@Test
	public void listUserAsNormalUser() throws ConfigurationException {

		Request request = createGetRequest();
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}

	private Request createGetRequest() throws ConfigurationException {
		return createGetRequest(user);
	}

	private Request createGetRequest(User user) throws ConfigurationException {
		Map<String, Object> attributes = new HashMap<String, Object>();
		Request request = createGetRequest(attributes);
		CookieUtils.addAuthnCookie(request, "", user.getName());
		return request;
	}

	private Response executeRequest(Request request) {
		return executeRequest(request, new UserListResource());
	}

}
