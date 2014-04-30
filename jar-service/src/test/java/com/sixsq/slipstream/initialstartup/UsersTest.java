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

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.exceptions.NotFoundException;

public class UsersTest {

	// Fixes #143
	@Test
	public void passwordIsPersisted() throws ValidationException,
			NotFoundException, ConfigurationException,
			NoSuchAlgorithmException, UnsupportedEncodingException {
		Users.create();

		User userSuper = User.loadByName("super");
		assertEquals(
				"304D73B9607B5DFD48EAC663544F8363B8A03CAAD6ACE21B369771E3A0744AAD0773640402261BD5F5C7427EF34CC76A2626817253C94D3B03C5C41D88C64399",
				userSuper.getPassword());

		userSuper.hashAndSetPassword("qwertz");
		userSuper.store();

		User userTest = User.loadByName("test");
		assertEquals(
				"4AE302CB035CB6466AE3C94613C52943E944694966C771194C70EC5C3C25E6E09713AD96592CCA942C698226CA865EE57B1BCB78A8F03B311320268E19CF08D9",
				userTest.getPassword());

		userTest.hashAndSetPassword("asdfgh");
		userTest.store();

		User userSixsq = User.loadByName(Users.SIXSQ);
		assertEquals(
				"448FC6AFDC6F117FC0DC7A1336EE33F7E3DA0ABB064F0EF354E61A101D0CB5EBACF0AA1AE41DE558C3A7074B597A2C4D4EFF8DADF06D9F806749A7C41669BD26",
				userSixsq.getPassword());

		userSixsq.hashAndSetPassword("yxcvbn");
		userSixsq.store();

		Users.create();

		assertEquals(
				"4AAEC1C5E8C60370F95D0935EFCAA3245736439203E91742D4686AA50C3FBA96A567909567BE623F033500591132DC5BDB8DDB27E0587DB97A986EC92245FC80",
				userSuper.getPassword());
		assertEquals(
				"2F5FB2A469A918A84C1A467D40E4CA2250F1BDAAE45C928D616E51E5D73DB7D56470EFF626211E710083957E3B40B6AC55B58D5130E02EDC4ACC1F3D91F5302A",
				userTest.getPassword());
		assertEquals(
				"457ADFBC3B4E473315B496835005EE06A808A912F03B6557DC21404F862F6F13AEFAC1E84788BFBBE475C1C5BAD6BD2E5C95842B5498EA8AF777541E71C59C4E",
				userSixsq.getPassword());
	}
}
