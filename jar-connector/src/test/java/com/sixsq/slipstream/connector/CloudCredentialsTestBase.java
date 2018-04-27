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
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.ssclj.app.CIMITestServer;
import com.sixsq.slipstream.util.CommonTestUtil;
import com.sixsq.slipstream.util.SscljProxy;
import org.junit.*;
import org.restlet.Response;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

public abstract class CloudCredentialsTestBase implements ICloudCredentialsTestBase {

	protected static User user;

	public static final String PASSWORD = "password";

	private static final String DEFAULT_CONNECTOR_NAME = "foo-bar-baz";

	private static String CONNECTOR_NAME;

	@BeforeClass
	public static void setupClass() {
		CIMITestServer.start();
		CljElasticsearchHelper.initTestDb();
		Configuration.refreshRateSec = 1;
		enableQuota();
	}

	private static void enableQuota() {
		HashMap conf = new HashMap<String, String>();
		conf.put("quotaEnable", true);
		String resource = "configuration/slipstream";
		Response resp = SscljProxy.put(SscljProxy.BASE_RESOURCE + resource, "super ADMIN", conf);
		if (SscljProxy.isError(resp)) {
			fail("Failed to update " + resource + " with: " + resp.getEntityAsText());
		}
		CIMITestServer.refresh();
		try {
			Thread.sleep(Configuration.refreshRateSec * 1000 + 100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void teardownClass() throws ValidationException {
		Configuration.getInstance().reinitialise();
		CIMITestServer.stop();
	}

	@Before
	public void setup() {
		user = createUser("test", PASSWORD, getConnectorName());
		user = storeUser(user);
		CIMITestServer.refresh();

		// Create connector.
		try {
			CommonTestUtil.createConnector(getCloudServiceName(), getConnectorName(),
					getSystemConfParams());
		} catch (ValidationException e) {
			e.printStackTrace();
			fail("Failed to create connector " + getConnectorName() + " with: " +
					e.getMessage());
		}
	}

	public  String getConnectorName(){
		return DEFAULT_CONNECTOR_NAME;
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
		Map<String, UserParameter> credParams = u1.getParameters(getConnectorName());
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
		return createUser(name, password, DEFAULT_CONNECTOR_NAME);
	}

	public static User createUser(String name, String password, String connectorName) {
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
			user.setDefaultCloudServiceName(connectorName);
		} catch (ValidationException e) {
			throw (new SlipStreamRuntimeException(e));
		}

		return user;
	}


}

