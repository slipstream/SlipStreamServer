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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User.State;
import com.sixsq.slipstream.user.UserView;
import com.sixsq.slipstream.util.ResourceTestBase;
import com.sixsq.slipstream.util.SerializationUtil;

public class UserTest extends ResourceTestBase {

	@Before
	public void setup(){
		storeUser(user);
	}
	
	@After
	public void tearDown(){
		user.remove();
	}
	
	@Test
	public void setParameter() throws ValidationException {
		User user = User.loadByName("test");
		
		user.setParameter(new UserParameter("p", "v", "d"));
		user = user.store();
		user.setParameter(new UserParameter("p", "v", "d"));
		user.store();
		user.remove();
	}
	
	@Test
	public void verifyCorrectName() throws SlipStreamClientException {

		String name = "dummy";
		String resourceUrl = User.constructResourceUri(name);

		User user = createUser(name);

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

		User user = createUser(name);
		user.store();

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

		User user = createUser(username);

		String resourceUrl = user.getResourceUri();

		String parameterName = "name";
		String description = "description";
		String value = "value";

		UserParameter parameter = new UserParameter(parameterName, value,
				description);
		user.setParameter(parameter);

		user.store();

		User restored = User.load(resourceUrl);
		assertNotNull(restored);

		Map<String, UserParameter> parameters = restored.getParameters();
		assertNotNull(parameters);
		assertTrue(parameters.size() > 0);

		parameter = parameters.get(parameterName);
		assertNotNull(parameter);
		assertEquals(parameterName, parameter.getName());
		assertEquals(description, parameter.getDescription());
		assertEquals(value, parameter.getValue());

		restored.remove();
		user = User.load(resourceUrl);
		assertNull(user);
	}

	@Test
	public void verifyUserList() {

		User user1 = createUser("user1");
		user1.setState(State.ACTIVE);
		user1.store();

		User user2 = createUser("user2");
		user2.setState(State.NEW);
		user2.store();

		User user3 = createUser("user3");
		user3.setState(State.ACTIVE);
		user3.store();

		Set<String> activeUsernames = new TreeSet<String>();
		activeUsernames.add("user1");
		activeUsernames.add("user3");

		List<User> userList = User.list();
		assertEquals(2, userList.size());

		Set<String> retrievedUsernames = new TreeSet<String>();
		for (User u : userList) {
			retrievedUsernames.add(u.getName());
		}

		assertEquals(activeUsernames, retrievedUsernames);

		User.removeNamedUser("user1");
		User.removeNamedUser("user2");
		User.removeNamedUser("user3");
	}

	@Test
	public void verifyUserViewList() {

		List<UserView> userViewList = User.viewList();
		int before = userViewList.size();

		User user1 = createUser("user1");
		user1.setState(State.ACTIVE);
		user1.store();

		User user2 = createUser("user2");
		user2.setState(State.NEW);
		user2.store();

		User user3 = createUser("user3");
		user3.setState(State.ACTIVE);
		user3.store();

		Set<String> allUsernames = new TreeSet<String>();
		allUsernames.add("user1");
		allUsernames.add("user2");
		allUsernames.add("user3");

		userViewList = User.viewList();
		assertThat(userViewList.size(), is(allUsernames.size()+before));

		User.removeNamedUser("user1");
		User.removeNamedUser("user2");
		User.removeNamedUser("user3");
	}

	@Test
	public void checkUserSerialization() {

		User user1 = createUser("user1");
		user1.setState(State.ACTIVE);
		user1.store();

		SerializationUtil.toXmlString(user1);
	}
}
