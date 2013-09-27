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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Status;

import com.sixsq.slipstream.connector.ExecutionControlUserParametersFactory;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.User.State;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.util.ResourceTestBase;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.XmlUtil;

public class UserResourceTest extends ResourceTestBase {

	private static final String NEW_PASSWORD = "newPassword";
	private static final String SUPER_PASSWORD = "passwordSuper";
	private static User otherUser = createUser("test2", "password2");
	private static User superUser = createUser("super", SUPER_PASSWORD);

	@BeforeClass
	public static void setupBeforeClass() throws InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {
		resetAndLoadConnector(LocalConnector.class);
	}

	@Before
	public void setup() {
		user = storeUser(user);
		storeUser(otherUser);
		superUser.setSuper(true);
		storeUser(superUser);
	}

	@After
	public void tearDown() {
		user.remove();
		otherUser.remove();
		superUser.remove();
	}

	@Test
	public void getUser() throws ConfigurationException {

		Request request = createGetRequest(user);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

	}

	@Test
	public void normalUserCantAccessOthers() throws ConfigurationException {

		Request request = createGetRequest(user, otherUser.getName());
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));
	}

	@Test
	public void getUserAsSuperUser() throws ConfigurationException {

		Request request = createGetRequest(superUser, otherUser.getName());
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}

	@Test
	public void getInexistantUser() throws ConfigurationException {

		Request request = createGetRequest(user, "inexistant");
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}

	@Test
	public void changePasswordValidAsSuper() throws ConfigurationException,
			NoSuchAlgorithmException, UnsupportedEncodingException {

		user = user.store();
		user.hashAndSetPassword("something old");
		user = user.store();
		assertThat(getPersistedPassword(user), is(not(Passwords.hash(NEW_PASSWORD))));

		Passwords passwords = createValidPasswords(PASSWORD);
		Request request = createPutRequest(user, superUser.getName(), passwords);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

		User modifiedUser = User.load(user.getResourceUri());

		assertThat(getPersistedPassword(modifiedUser), is(Passwords.hash(NEW_PASSWORD)));
	}

	@Test
	public void changePasswordValidAsItself() throws ConfigurationException,
			NoSuchAlgorithmException, UnsupportedEncodingException {

		user = user.store();
		String oldPassword = "something old";
		user.hashAndSetPassword(oldPassword);
		user = user.store();
		assertThat(getPersistedPassword(user), is(not(NEW_PASSWORD)));

		Passwords passwords = createValidPasswords(oldPassword);
		Request request = createPutRequest(user, user.getName(), passwords);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

		User modifiedUser = User.load(user.getResourceUri());

		assertThat(getPersistedPassword(modifiedUser), is(Passwords.hash(NEW_PASSWORD)));
	}

	@Test
	public void cannotChangePasswordOfOtherUser()
			throws ConfigurationException, NoSuchAlgorithmException,
			UnsupportedEncodingException {

		String oldPassword = "old password";
		user.hashAndSetPassword(oldPassword);
		user = (User) user.store();
		assertThat(getPersistedPassword(user), is(not(NEW_PASSWORD)));

		Passwords passwords = createValidPasswords(oldPassword);
		Request request = createPutRequest(user, otherUser.getName(), passwords);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));
	}

	@Test
	public void changePasswordMissingOldPassword()
			throws ConfigurationException, NoSuchAlgorithmException,
			UnsupportedEncodingException {

		Passwords passwords = createValidPasswords(PASSWORD);
		passwords.oldPassword = null;
		Request request = createPutRequest(user, user.getName(), passwords);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_CONFLICT));
	}

	@Test
	public void superCanChangeOthersPasswordWithoutOldPassword()
			throws ConfigurationException, ValidationException,
			NoSuchAlgorithmException, UnsupportedEncodingException {

		user.hashAndSetPassword(PASSWORD);

		Passwords passwords = createValidPasswords(PASSWORD);
		passwords.oldPassword = null;
		Request request = createPutRequest(user, superUser.getName(), passwords);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

		user = User.load(user.getResourceUri());
		assertThat(user.getPassword(), is(Passwords.hash(NEW_PASSWORD)));
	}

	@Test
	public void superMustProvideItsOwnOldPassowrdToChangeItsPassword()
			throws ConfigurationException, ValidationException,
			NoSuchAlgorithmException, UnsupportedEncodingException {

		Passwords passwords = createValidPasswords(SUPER_PASSWORD);
		passwords.oldPassword = null;
		Request request = createPutRequest(superUser, superUser.getName(),
				passwords);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_CONFLICT));
	}

	@Test
	public void passwordBlankedForNormalUser() throws SlipStreamClientException {
		Request request = createGetRequest(user, user.getName());
		Response response = executeRequest(request);

		User user = (User) SerializationUtil.fromXml(
				response.getEntityAsText(), User.class);
		assertNull(user.getPassword());
	}

	@Test
	@Ignore
	public void passwordSerializedForNormalUserAsSuper()
			throws SlipStreamClientException {

		Request request = createGetRequest(user, superUser.getName());
		Response response = executeRequest(request);

		User user = (User) SerializationUtil.fromXml(response.toString(),
				User.class);
		assertNotNull(user.getPassword());
	}

	@Test
	public void passwordBlankedForSuperAsSuper()
			throws SlipStreamClientException {

		Request request = createGetRequest(superUser, superUser.getName());
		Response response = executeRequest(request);

		User user = (User) SerializationUtil.fromXml(
				response.getEntityAsText(), User.class);
		assertNull(user.getPassword());
	}

	@Test
	public void deleteUser() throws ConfigurationException, ValidationException {

		User toBeDelete = new User("toBeDelete");
		storeUser(toBeDelete);

		Request request = createDeleteRequest(toBeDelete, toBeDelete);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_NO_CONTENT));
	}

	@Test
	public void cantDeleteAnotherUser() throws ConfigurationException,
			ValidationException {

		User cantBeDeleted = new User("cantBeDeletedByAnotherUser");
		storeUser(cantBeDeleted);

		Request request = createDeleteRequest(user, cantBeDeleted);
		Response response = executeRequest(request);

		cantBeDeleted.remove();

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));
	}

	@Test
	public void cantSelfAssignSuperUnlessAlreadySuper()
			throws ConfigurationException, ValidationException {

		User cantSelfAssignSuper = new User("cantSelfAssignSuper");
		storeUser(cantSelfAssignSuper);

		cantSelfAssignSuper.setSuper(true);

		Request request = createPutRequest(cantSelfAssignSuper,
				"cantSelfAssignSuper");
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

		cantSelfAssignSuper = User.loadByName("cantSelfAssignSuper");
		cantSelfAssignSuper.remove();

		assertThat(cantSelfAssignSuper.isSuper(), is(false));

	}

	@Test
	public void superCanAssignSuperToOthers() throws ConfigurationException,
			ValidationException {

		User superCanAssignSuperToOthers = new User(
				"superCanAssignSuperToOthers");

		superCanAssignSuperToOthers.setSuper(true);

		storeUser(superCanAssignSuperToOthers);

		superCanAssignSuperToOthers.setSuper(false);

		Request request = createPutRequest(superCanAssignSuperToOthers,
				superUser.getName());
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

		superCanAssignSuperToOthers = User
				.loadByName("superCanAssignSuperToOthers");
		superCanAssignSuperToOthers.remove();

		assertThat(superCanAssignSuperToOthers.isSuper(), is(false));

	}

	private String getPersistedPassword(User user) {
		User restoredUser = User.load(user.getResourceUri());
		return restoredUser.getPassword();
	}

	private Passwords createValidPasswords(String oldPassword)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		Passwords passwords = new Passwords(oldPassword, NEW_PASSWORD,
				NEW_PASSWORD);
		return passwords;
	}

	@Test
	public void getNewToRetrieveTemplate() throws ConfigurationException {

		Request request = createGetRequest(user, "new");
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}

	@Test
	public void editRedirectsToView() throws ConfigurationException {

		Passwords passwords = new Passwords();

		Request request = createPutRequest(user, user.getName(), passwords);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
		assertThat(response.getLocationRef().getPath(), is("/user/test"));
	}

	@Test
	public void putWithParameter() throws ConfigurationException,
			ValidationException {

		String paramName = ExecutionControlUserParametersFactory.CATEGORY
				+ "."
				+ ExecutionControlUserParametersFactory.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME;
		UserParameter p = new UserParameter(paramName,
				LocalConnector.CLOUD_SERVICE_NAME, "description");
		user.setParameter(p);
		Request request = createPutRequest(user, user.getName());
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

		User updated = User.load(user.getResourceUri());

		assertThat(updated.getParameter(paramName).getValue(),
				is(LocalConnector.CLOUD_SERVICE_NAME));
	}

	@Test
	public void putWithUserState() throws ValidationException,
			ConfigurationException {
		User withState = new User("getUserState");
		withState.setState(State.SUSPENDED);

		Request request = createPutRequest(withState, superUser.getName());
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));

		withState = User.loadByName(withState.getName());
		assertThat(withState.getState(), is(State.SUSPENDED));

		withState.remove();
	}

	@Test
	public void systemParameterMerge() throws SlipStreamClientException {
		Request request = createGetRequest(superUser, otherUser.getName());

		// Pick a category that we know always exists
		String category = "SlipStream_Support";
		CookieUtils.addAuthnCookie(request, otherUser.getName(), category);

		String cookieCategory = CookieUtils.getCookieCloudServiceName(request
				.getCookies().getFirst(CookieUtils.getCookieName()));

		// need to add the cloud service name directly as an attribute, since
		// we're not going through the CookieAuthenticator
		request.getAttributes().put(RuntimeParameter.CLOUD_SERVICE_NAME,
				cookieCategory);
		Response response = executeRequest(request);

		String denormalized = XmlUtil.denormalize(response.getEntityAsText());
		User user = (User) SerializationUtil.fromXml(denormalized, User.class);

		UserParameter systemParameter = user
				.getParameter("slipstream.support.email");
		assertNotNull(systemParameter);
	}

	private Request createDeleteRequest(User targetUser, User user)
			throws ConfigurationException {
		return createDeleteRequest(targetUser, user.getName());
	}

	private Request createDeleteRequest(User user, String targetUsername)
			throws ConfigurationException {
		Map<String, Object> attributes = createUserAttributes(targetUsername);
		Request request = createDeleteRequest(attributes);
		addUserToRequest(user, request);
		return request;
	}

	private Request createPutRequest(User targetUser, String username)
			throws ConfigurationException {
		Passwords passwords = new Passwords();
		return createPutRequest(targetUser, username, passwords);
	}

	private Request createPutRequest(User targetUser, String username,
			Passwords passwords) throws ConfigurationException {
		Form form = new Form();
		form.add("name", targetUser.getName());
		form.add("firstName", targetUser.getFirstName());
		form.add("lastName", targetUser.getLastName());

		form.add("password1", passwords.newPassword1);
		form.add("password2", passwords.newPassword2);
		form.add("oldPassword", passwords.oldPassword);

		if (targetUser.isSuper()) {
			form.add("super", "on");
		}

		form.add("state", targetUser.getState().toString());

		for (UserParameter parameter : targetUser.getParameters().values()) {
			UserFormProcessorTest.fillForm(parameter, form);
		}

		Map<String, Object> attributes = createUserAttributes(targetUser
				.getName());

		Request request = createPutRequest(attributes,
				form.getWebRepresentation(), targetUser.getResourceUri());

		addUserToRequest(username, request);
		return request;
	}

	private Request createGetRequest(User user) throws ConfigurationException {
		return createGetRequest(user, user.getName());
	}

	private Request createGetRequest(User user, String targetUsername)
			throws ConfigurationException {
		Map<String, Object> attributes = createUserAttributes(targetUsername);
		Request request = createGetRequest(attributes);
		addUserToRequest(user, request);
		return request;
	}

	private Map<String, Object> createUserAttributes(String targetUsername) {
		return createAttributes("user", targetUsername);
	}

	private Response executeRequest(Request request) {
		return executeRequest(request, new UserResource());
	}

}
