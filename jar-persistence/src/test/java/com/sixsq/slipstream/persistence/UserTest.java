package com.sixsq.slipstream.persistence;

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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.*;
import com.sixsq.slipstream.persistence.User.State;
import com.sixsq.slipstream.ssclj.app.SscljTestServer;
import com.sixsq.slipstream.user.UserView;
import com.sixsq.slipstream.util.SerializationUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class UserTest {

	protected static User user;

	public static final String PASSWORD = "password";

	@BeforeClass
	public static void setupClass() {
		SscljTestServer.start();
	}

	@AfterClass
	public static void teardownClass() {
		SscljTestServer.stop();
	}

	@Before
	public void setup() {
		user = createUser("test");
		user = storeUser(user);
		SscljTestServer.refresh();
	}

	public static User storeUser(User user) {
		return user.store();
	}

	@After
	public void tearDown() {
	    removeAllUsers();
	}

	public void removeAllUsers() {
		SscljTestServer.refresh();
		for(User u : User.list()) {
			u.remove();
		}
		SscljTestServer.refresh();
	}

	@Test
	public void loadByName() throws ValidationException {
	    String username = UserTest.user.getName();
		User user = User.loadByName(username);
		assertTrue(username.equals(user.getName()));
	}

	@Test
	public void verifyCorrectName() throws SlipStreamClientException {

		String name = "dummy";
		String resourceUrl = User.constructResourceUri(name);

		User user = UserTest.createUser(name);

		assertEquals(name, user.getName());
		assertEquals(resourceUrl, user.getResourceUri());
	}

	@Test(expected = InvalidElementException.class)
	public void nameCannotBeNull() throws InvalidElementException,
			ValidationException {
		User.validateMinimumInfo(new User(null));
	}

	@Test(expected = ValidationException.class)
	public void emptyNameNotAllowed() throws SlipStreamClientException {
		User user = new User("");
		user.validate();
	}

	@Test
	public void storeRetrieveAndDelete() throws SlipStreamClientException {

		String name = "dummy";
		String resourceUrl = User.constructResourceUri(name);

		User user = UserTest.createUser(name);
		user.store();
		SscljTestServer.refresh();

		User userRestored = User.loadByName(name);
		assertNotNull(userRestored);

		assertEquals(user.getName(), userRestored.getName());
		assertEquals(user.getResourceUri(), userRestored.getResourceUri());

		userRestored = User.load(resourceUrl);

		assertEquals(user.getName(), userRestored.getName());
		assertEquals(user.getResourceUri(), userRestored.getResourceUri());

		userRestored.remove();
		userRestored = User.load(resourceUrl);
		assertNull(userRestored);
	}

	@Test
	public void withParameters() throws SlipStreamClientException {

		String username = "dummy";

		User user = UserTest.createUser(username).store();
		SscljTestServer.refresh();

		String resourceUrl = user.getResourceUri();

		String category = ParameterCategory.General.toString();
		String parameterName = UserParameter.constructKey(category,
				UserParameter.KEY_KEEP_RUNNING);
		String value = "always";

		UserParameter parameter = new UserParameter(parameterName, value, "");
		parameter.setCategory(category);
		user.setParameter(parameter);

		user.store();
		SscljTestServer.refresh();

		User restored = User.load(resourceUrl);
		assertNotNull(restored);

		Map<String, UserParameter> parameters = restored.getParameters();
		assertNotNull(parameters);
		assertTrue(parameters.size() > 0);

		parameter = parameters.get(parameterName);
		assertNotNull(parameter);
		assertEquals(parameterName, parameter.getName());
		assertEquals(value, parameter.getValue());
		assertFalse(parameter.getDescription().isEmpty());

		restored.remove();
		SscljTestServer.refresh();
		user = User.load(resourceUrl);
		assertNull(user);
	}

	@Test
	public void verifyUserList() {

		User userActive1 = UserTest.createUser("user1").store();
		userActive1.setState(State.ACTIVE);
		userActive1.store();

		User userNew = UserTest.createUser("user2").store();
		userNew.setState(State.NEW);
		userNew.store();

		User userActive2 = UserTest.createUser("user3").store();
		userActive2.setState(State.ACTIVE);
		userActive2.store();

		Set<String> activeUsernames = new TreeSet<String>();
		activeUsernames.add("user1");
		activeUsernames.add("user3");

		List<User> allUsers = User.list();
		assertEquals(4, allUsers.size()); // 3 + 1 from the setup

		List<User> userList = User.listActive();
		assertEquals(2, userList.size());

		Set<String> retrievedUsernames = new TreeSet<String>();
		for (User u : userList) {
			retrievedUsernames.add(u.getName());
		}

		assertEquals(activeUsernames, retrievedUsernames);
	}

	@Test
	public void verifyUserViewList() {

		List<UserView> userViewList = User.viewList();
		int before = userViewList.size();

		User user1 = UserTest.createUser("user1");
		user1.setState(State.ACTIVE);
		user1.store();

		User user2 = UserTest.createUser("user2");
		user2.setState(State.NEW);
		user2.store();

		User user3 = UserTest.createUser("user3");
		user3.setState(State.ACTIVE);
		user3.setLastModified();
		user3.store();

		Set<String> allUsernames = new TreeSet<String>();
		allUsernames.add("user1");
		allUsernames.add("user2");
		allUsernames.add("user3");

		userViewList = User.viewList();
		assertThat(userViewList.size(), is(allUsernames.size() + before));
	}

	@Test
	public void userViewListHasAttributes() {

		User user = UserTest.createUser("user").store();
		user.setState(State.ACTIVE);
		user.setLastModified();
		user.store();

		List<UserView> userViewList = User.viewList();
		UserView userView = userViewList.get(1);
		assertNotNull(userView.activeSince);
	}

	@Test
	public void checkUserSerialization() {

		User user1 = UserTest.createUser("user1");
		user1.setState(State.ACTIVE);
		user1.store();

		SerializationUtil.toXmlString(user1);
	}

	@Test(expected = ValidationException.class)
	public void putEmptyUserFailsValidation() throws ConfigurationException,
			ValidationException {

		User user = new User(null);
		user.validate();
	}

	@Test(expected = ValidationException.class)
	public void illegalUserName() throws ConfigurationException,
			ValidationException {

		User user = new User(User.NEW_NAME);
		user.validate();
	}

	@Test
	public void rolesShouldBeValidNamesCommaSeparated() {
		assert(validateRoles(""));
		assert(validateRoles("A"));
		assert(validateRoles("cyclone-fr1"));
		assert(validateRoles("exoscale,  cyclone-fr1"));
		assert(validateRoles("exoscale  		,	  	cyclone-fr1")); // tabs
		assert(validateRoles("exoscale, cyclone-fr1, ec2-ap-northeast-1, ec2-ap-southeast-1"));

		assert(validateRoles(",,,"));
		assert(validateRoles("cyclone._--__...fr1"));

		assertFalse(validateRoles("exoscale  cyclone-fr1"));
		assertFalse(validateRoles("1abc"));
		assertFalse(validateRoles("exoscale%"));
	}

	@Test
	public void forbiddenRolesAreRejected() {
		assertFalse(validateRoles("ADMIN"));
		assertFalse(validateRoles("USER"));
		assertFalse(validateRoles("ROLE"));
		assertFalse(validateRoles("ANON"));
		assertFalse(validateRoles("role1,ANON"));
		assertFalse(validateRoles("ANON, role1"));
	}

	private boolean validateRoles(String roles) {
		try {
			User user = UserTest.createUser("testingroles");
			user.setRoles(roles);
			user.validate();
			return true;
		} catch (ValidationException e) {
			return false;
		}
	}

	@Test
	public void isSuperAlone() {
		removeAllUsers();
		assertFalse(User.isSuperAlone());
		UserTest.createUser("user").store();
		SscljTestServer.refresh();
		assertFalse(User.isSuperAlone());
		UserTest.createUser("super").store();
		SscljTestServer.refresh();
		assertFalse(User.isSuperAlone());
		removeAllUsers();
		UserTest.createUser("super").store();
		SscljTestServer.refresh();
		assertTrue(User.isSuperAlone());
	}

	public static User createUser(String name, String password) {
		User user = null;
		try {
			user = User.loadByName(name);
		} catch (ConfigurationException|ValidationException e) {
			e.printStackTrace();
			fail();
		}
		if (user != null) {
			user.remove();
			SscljTestServer.refresh();
		}
		try {
			user = new User(name);
		} catch (ValidationException e) {
			e.printStackTrace();
			fail();
		}

		user.setFirstName("Te");
		user.setLastName("st");
		user.setEmail("test@example.com");

		try {
			user.setKeepRunning(UserParameter.KEEP_RUNNING_ON_ERROR);
		} catch (ValidationException e) {
			e.printStackTrace();
			fail();
		}

		try {
			user.hashAndSetPassword(password);
		} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}

		return user;
	}

	public static User createUser(String name) {
		return createUser(name, PASSWORD);
	}
}
