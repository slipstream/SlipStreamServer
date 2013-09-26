package com.sixsq.slipstream.initialstartup;

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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.User.State;

// FIXME: This class should be moved into a more complete integration test.
public class Users {

	public static void create() throws ValidationException, NotFoundException,
			ConfigurationException {
		createSuperUser();
		createTestUser();
	}

	private static void createSuperUser() throws ValidationException {
		User user = createUser("super");
		user.setFirstName("Super");
		user.setLastName("User");
		user.setEmail("super@sixsq.com");
		user.setOrganization("SixSq");
		try {
			user.hashAndSetPassword("supeRsupeR");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new ValidationException(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new ValidationException(e.getMessage());
		}
		user.setState(State.ACTIVE);
		user.setSuper(true);

		user.store();
	}

	private static void createTestUser() throws ValidationException {
		User user = User.loadByName("test");
		if (user != null) {
			return;
		}
		user = createUser("test");
		user.setFirstName("Test");
		user.setLastName("User");
		user.setEmail("test@sixsq.com");
		user.setOrganization("SixSq");
		try {
			user.hashAndSetPassword("tesTtesT");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new ValidationException(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new ValidationException(e.getMessage());
		}
		user.setState(State.ACTIVE);
		user.setSuper(false);

		user.store();

		user = User.loadByName("test");
	}

	private static User createUser(String name) {
		User user = null;
		try {
			user = new User(name);
		} catch (ValidationException e) {
			throw (new SlipStreamRuntimeException(e));
		}
		return user;
	}

	public static void createServiceConfiguration()
			throws ConfigurationException {

		ServiceConfiguration cfg = Configuration.getInstance().getParameters();

		cfg.store();
	}
}
