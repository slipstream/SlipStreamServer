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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.w3c.dom.Document;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.SerializationUtil;

public class UserParameterTest {

	@Test(expected = ValidationException.class)
	public void nullParameterName() throws SlipStreamClientException {

		new UserParameter(null, "ok", "ok");
	}

	@Test
	public void serializationWorks() throws ValidationException {
		UserParameter parameter = new UserParameter("dummy", "ok", "ok");

		String serialization = SerializationUtil.toXmlString(parameter);

		assertNotNull(serialization);
		assertFalse("".equals(serialization));

		Document document = SerializationUtil.toXmlDocument(parameter);

		assertNotNull(document);
	}

	@Test
	public void parametersPersisted() throws ValidationException {
		User user = new User("parametersPersisted");

		UserParameter p = new UserParameter("toto");
		user.setParameter(p);

		user.store();

		user = User.loadByName(user.getName());

		assertThat(user.getParameters().size(), is(greaterThan(0)));

		user.remove();
	}

	@Test
	public void convertOldFormatToKeepRunning() {
		assertThat(UserParameter.convertOldFormatToKeepRunning(false, false), is(UserParameter.KEEP_RUNNING_NEVER));
		assertThat(UserParameter.convertOldFormatToKeepRunning(false, true), is(UserParameter.KEEP_RUNNING_ON_ERROR));
		assertThat(UserParameter.convertOldFormatToKeepRunning(true, false), is(UserParameter.KEEP_RUNNING_ON_SUCCESS));
		assertThat(UserParameter.convertOldFormatToKeepRunning(true, true), is(UserParameter.KEEP_RUNNING_ALWAYS));
	}
}
