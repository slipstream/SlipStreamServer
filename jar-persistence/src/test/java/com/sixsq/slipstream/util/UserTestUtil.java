package com.sixsq.slipstream.util;

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
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.fail;

public abstract class UserTestUtil {

	protected static final String PASSWORD = "password";
	protected static final String publicSshKey =  "ssh-rsa ABCD x";

	public static User createTestUser() throws ConfigurationException,
			ValidationException {
		return createUserWithExecParams("test", PASSWORD);
	}

	public static User createUserWithExecParams(String name) throws ConfigurationException,
			ValidationException {
		return UserTestUtil.createUserWithExecParams(name, "");
	}

	public static User createMinimalUser(String name, String password) throws ValidationException {

		User user = User.loadByName(name);
		if (user != null) {
			try {
				user.remove();
			} catch (Exception ex) {

			}
		}

		try {
			user = new User(name);
		} catch (ValidationException e) {
			e.printStackTrace();
			throw (new SlipStreamRuntimeException(e));
		}
		try {
			user.hashAndSetPassword(password);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}

		user.setFirstName("Te");
		user.setLastName("st");
		user.setEmail("test@example.com");

		return user.store();
	}

	public static User createUserWithExecParams(String name, String password)
			throws ConfigurationException, ValidationException {
		User user;

		user = createMinimalUser(name, password);

		try {
			user.setDefaultCloudServiceName("dummy");
		} catch (ValidationException e) {
			throw (new SlipStreamRuntimeException(e));
		}

		user.setKeepRunning(UserParameter.KEEP_RUNNING_NEVER);

		String key = UserParametersFactoryBase.getPublicKeyParameterName();
		user.setParameter(new UserParameter(key, publicSshKey, ""));

		return user.store();
	}

	public static void deleteUser(User user) {
		if (user != null) {
			user.remove();
		}
	}

	public static void addSshKeys(User user) throws ValidationException {
		UserParameter userKey = new UserParameter(UserParametersFactoryBase
				.getPublicKeyParameterName(), publicSshKey, "xxx");
		user.setParameter(userKey);

		String publicSshKey = ServiceConfiguration.CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY;
		Configuration config = Configuration.getInstance();

		config.getParameters().setParameter(new ServiceConfigurationParameter(publicSshKey, "/dev/null"));
		config.store();
	}

	// Only static methods. Ensure no instances are created.
	public UserTestUtil() {

	}

}
