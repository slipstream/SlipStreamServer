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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sixsq.slipstream.es.CljElasticsearchHelper;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.ssclj.app.SscljTestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.exceptions.NotFoundException;

public class UsersTest {

	@BeforeClass
	public static void setupClass() {
		SscljTestServer.start();
	}

	@AfterClass
	public static void teardownClass() {
		SscljTestServer.stop();
	}

	// Fixes #143
	@Test
	public void passwordIsPersisted() throws ValidationException,
			NotFoundException, ConfigurationException,
			NoSuchAlgorithmException, UnsupportedEncodingException {

        // The create function will not initialize the accounts if they
        // already exist.  Ensure that all of the standard accounts have
        // been removed from the database.
        if (User.loadByName("super") != null) {
            User.removeNamedUser("super");
        }

		Users.create();

		User userSuper = User.loadByName("super");
		assertEquals(
				"304D73B9607B5DFD48EAC663544F8363B8A03CAAD6ACE21B369771E3A0744AAD0773640402261BD5F5C7427EF34CC76A2626817253C94D3B03C5C41D88C64399",
				userSuper.getHashedPassword());

		userSuper.hashAndSetPassword("qwertz");
		userSuper.store();

		Users.create();

		assertEquals(
				"4AAEC1C5E8C60370F95D0935EFCAA3245736439203E91742D4686AA50C3FBA96A567909567BE623F033500591132DC5BDB8DDB27E0587DB97A986EC92245FC80",
				userSuper.getHashedPassword());
	}

	@Test(expected = InvalidElementException.class)
	public void passwordCannotBeEmpty() throws ValidationException, NotFoundException, ConfigurationException,
			NoSuchAlgorithmException, UnsupportedEncodingException, InvalidElementException {
		// The create function will not initialize the accounts if they
		// already exist.  Ensure that all of the standard accounts have
		// been removed from the database.
		if (User.loadByName("super") != null) {
			User.removeNamedUser("super");
		}

		Users.create();

		User user = User.loadByName("super");
		user.setPassword(null);
		user.validate();
		User.validateMinimumInfo(user);
	}

	@Test
	public void loadSingleUserFromConfigFiles() throws ValidationException {
		ClassLoader classLoader = getClass().getClassLoader();
		String fn = classLoader.getResource("config/users/test.xml").getFile();
		File f = new File(fn);
		Users.loadSingleUser(f);
		CljElasticsearchHelper.dumpEsDb("user");
		User user = User.loadByName("test");
		assertTrue("test".equals(user.getName()));
		assertTrue("user/test".equals(user.getResourceUri()));
	}
}
