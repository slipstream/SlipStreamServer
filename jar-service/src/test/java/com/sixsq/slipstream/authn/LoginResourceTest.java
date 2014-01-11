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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.security.Verifier;

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.ResourceTestBase;

public class LoginResourceTest extends ResourceTestBase {

	@Before
	public void setUpBeforeClass() throws Exception {
		createAndStoreUser();
	}

	private void createAndStoreUser() throws NoSuchAlgorithmException,
			UnsupportedEncodingException, ConfigurationException,
			ValidationException {
		user = createUser("test");
		user.hashAndSetPassword("password");
		user.store();
	}

	@After
	public void tearDownAfterClass() throws Exception {
		user.remove();
	}

	@Test
	public void loginValidUser() throws ConfigurationException,
			ValidationException {

		Request request = createPostRequest(user);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

	}

	@Test
	public void loginValidUserWithCookiePresent()
			throws ConfigurationException, ValidationException {

		Request request = createPostRequest(user);
		Response response = executeRequest(request);

		Cookie cookie = response.getCookieSettings().getFirst(
				CookieUtils.getCookieName());

		assertNotNull(cookie);

		assertThat(CookieUtils.verifyAuthnCookie(cookie),
				is(Verifier.RESULT_VALID));

	}

	@Test
	public void loginInvalidUserNoCookie() throws ConfigurationException,
			ValidationException {
		User invalid = createUser("invalid", "password");
		Request request = createPostRequest(invalid);
		Response response = executeRequest(request);

		Cookie cookie = response.getCookieSettings().getFirst(
				CookieUtils.getCookieName());

		assertNull(cookie);
	}

	@Test
	public void loginInvalidUser() throws ConfigurationException,
			ValidationException {
		User invalid = createUser("invalid", "password");
		Request request = createPostRequest(invalid);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_UNAUTHORIZED));
	}

	@Test
	public void loginInvalidPasswordNoCookie() throws ConfigurationException,
			ValidationException {
		User invalid = createUser(user.getName(), "wrong");
		Request request = createPostRequest(invalid);
		Response response = executeRequest(request);

		Cookie cookie = response.getCookieSettings().getFirst(
				CookieUtils.getCookieName());

		assertNull(cookie);
	}

	@Test
	public void loginWrongPasswordUser() throws ConfigurationException,
			ValidationException {
		User wrongPassword = createUser(user.getName(), "wrong");
		Request request = createPostRequest(wrongPassword);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_UNAUTHORIZED));
	}

	@Test
	public void redirectsOnSuccessHtmlLogin() throws ConfigurationException,
			ValidationException {
		Request request = new Request(Method.POST,
				"http://something.org/test/request?redirectURL=/module");
		request.setRootRef(new Reference("http://something.org"));

		Representation entity = createUserLoginForm(user)
				.getWebRepresentation();
		entity.setMediaType(MediaType.TEXT_HTML);

		request.setEntity(entity);

		Preference<MediaType> mediaType = new Preference<MediaType>();
		mediaType.setMetadata(MediaType.TEXT_HTML);
		request.getClientInfo().getAcceptedMediaTypes().add(mediaType);

		RequestUtil.addConfigurationToRequest(request);

		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.REDIRECTION_SEE_OTHER));
		assertThat(response.getLocationRef().getPath(), is("/module"));
	}

	private Request createPostRequest(User user) throws ConfigurationException,
			ValidationException {
		Map<String, Object> attributes = new HashMap<String, Object>();
		Form form = createUserLoginForm(user);
		return createPostRequest(attributes, form.getWebRepresentation());
	}

	private Form createUserLoginForm(User user) {
		Form form = new Form();
		form.add("username", user.getName());
		form.add("password", PASSWORD);
		return form;
	}

	protected Response executeRequest(Request request) {
		return executeRequest(request, new LoginResource());
	}

}
