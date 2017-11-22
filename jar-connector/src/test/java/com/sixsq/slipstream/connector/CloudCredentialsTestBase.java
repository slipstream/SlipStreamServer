package com.sixsq.slipstream.connector;

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
import com.sixsq.slipstream.exceptions.NotImplementedException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.ssclj.app.SscljTestServer;
import com.sixsq.slipstream.util.CommonTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class CloudCredentialsTestBase implements
		ICloudCredentialsTestBase {

	protected static User user;

	public static final String PASSWORD = "password";

	public static final String CONNECTOR_NAME = "foo-bar-baz";

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

		// Create connector.
		try {
			CommonTestUtil.createConnector(getCloudServiceName(), CONNECTOR_NAME,
					getSystemConfParams());
		} catch (ValidationException e) {
			e.printStackTrace();
			fail("Failed to create connector " + CONNECTOR_NAME + " with: " +
					e.getMessage());
		}
	}

	@Override
	public String getCloudServiceName() {
		throw new NotImplementedException("Not implemented.");
	}

	@Override
	public SystemConfigurationParametersFactoryBase getSystemConfParams()
			throws ValidationException {
		throw new NotImplementedException("Not implemented.");
	}

	@Override
	public Map<String, UserParameter> createAndStoreCloudCredentials() throws ValidationException {
		throw new NotImplementedException("Not implemented.");
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

	public static boolean isInteger(String v) {
		try {
			Integer.parseInt(v);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Test
	public void cloudCredentialsLifecycle() throws ValidationException {

		Map<String, UserParameter> params = createAndStoreCloudCredentials();
		SscljTestServer.refresh();

		// Loaded user has the cloud credential parameters.
		User u1 = User.loadByName(user.getName());
		Map<String, UserParameter> credParams = u1.getParameters(CONNECTOR_NAME);
		assertNotNull(credParams);
		assertTrue(credParams.size() >= params.size());
		for (String pname: params.keySet()) {
			assertTrue(params.get(pname).getValue().equals(u1.getParameter(pname).getValue()));
		}

		// Credential parameters are updated.
        // Use integer to let integer based parameters to work.
		String newValue = String.valueOf(System.currentTimeMillis());
		newValue = newValue.substring(newValue.length() - 7);
		for (String pname: params.keySet()) {
			UserParameter p = user.getParameter(pname);
			p.setValue(newValue);
			user.setParameter(p);
			user.store();
			SscljTestServer.refresh();
			u1 = User.loadByName(user.getName());
			assertTrue(u1.getParameter(pname).getValue().equals(newValue));
		}
	}

	public static User createUser(String name, String password) {
		User user = null;
		try {
			user = User.loadByName(name);
		} catch (ConfigurationException |ValidationException e) {
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
		} catch (NoSuchAlgorithmException |UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}

		try {
			user.setDefaultCloudServiceName(CONNECTOR_NAME);
		} catch (ValidationException e) {
			throw (new SlipStreamRuntimeException(e));
		}

		return user;
	}

	public static User createUser(String name) {
		return createUser(name, PASSWORD);
	}
}

