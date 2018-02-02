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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.es.CljElasticsearchHelper;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotImplementedException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.ssclj.app.CIMITestServer;
import com.sixsq.slipstream.util.CommonTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class CloudCredentialsTestBase implements
		ICloudCredentialsTestBase {

	protected static User user;

	public static final String PASSWORD = "password";

	public static final String CONNECTOR_NAME = "foo-bar-baz";

	@BeforeClass
	public static void setupClass() throws ValidationException {
		CIMITestServer.start();
		CljElasticsearchHelper.initTestDb();
		enableQuota();
	}

	public static void enableQuota() throws ValidationException {
		ServiceConfiguration sc = Configuration.getInstance().getParameters();
		String scQuotaParamName = ServiceConfiguration.RequiredParameters.SLIPSTREAM_QUOTA_ENABLE.getName();
		ServiceConfigurationParameter quotaParam = sc.getParameter(scQuotaParamName);
		quotaParam.setValue("true");
		sc.setParameter(quotaParam);
		sc.store();
		CIMITestServer.refresh();
	}

	@AfterClass
	public static void teardownClass() {
		CIMITestServer.stop();
	}

	@Before
	public void setup() {
		user = createUser("test");
		user = storeUser(user);
		CIMITestServer.refresh();

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
		CIMITestServer.refresh();
		for(User u : User.list()) {
			u.remove();
		}
		CIMITestServer.refresh();
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
		CIMITestServer.refresh();

		// Loaded user has the cloud credential parameters.
		User u1 = User.loadByName(user.getName());
		Map<String, UserParameter> credParams = u1.getParameters(CONNECTOR_NAME);
		assertNotNull(credParams);
		assertTrue(credParams.size() >= params.size());
		for (String pname: params.keySet()) {
			String pVal = params.get(pname).getValue();
			assertNotNull(pVal);
			UserParameter u1Param = u1.getParameter(pname);
			assertNotNull(u1Param);
			assertTrue(pVal.equals(u1Param.getValue()));
		}

		// Credential parameters are updated.
        // Use integer to let integer based parameters to work.
		String newValue = String.valueOf(new Random().nextInt(100));
		for (String pname: params.keySet()) {
			UserParameter p = user.getParameter(pname);
			p.setValue(newValue);
			user.setParameter(p);
			user.store();
			CIMITestServer.refresh();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			u1 = User.loadByName(user.getName());
			String value = u1.getParameter(pname).getValue();
			assertTrue("Not equal for parameter: " + pname + " - " + value + " != " + newValue,
					value.equals(newValue));
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
			CIMITestServer.refresh();
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

