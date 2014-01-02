package com.sixsq.slipstream.credential;

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationTargetException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sixsq.slipstream.connector.ParametersFactory;
import com.sixsq.slipstream.connector.stratuslab.StratusLabConnector;
import com.sixsq.slipstream.connector.stratuslab.StratusLabUserParametersFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.util.ResourceTestBase;

public class StratusLabUserParametersFactoryTest {

	private static User user;

	@BeforeClass
	public static void setupClass() throws ValidationException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {
		ResourceTestBase
				.resetAndLoadConnector(com.sixsq.slipstream.connector.stratuslab.StratusLabConnector.class);

		user = new User("StratusLabUserParametersFactoryTest");
		user.setDefaultCloudServiceName(StratusLabConnector.CLOUD_SERVICE_NAME);
		user = (User) user.store();

	}

	@Test
	public void defaultParameters() throws ConfigurationException,
			ValidationException {
		ParametersFactory.addParametersForEditing(user);

		String paramName = new StratusLabUserParametersFactory(StratusLabConnector.CLOUD_SERVICE_NAME)
				.constructKey(StratusLabUserParametersFactory.KEY_PARAMETER_NAME);
		UserParameter parameter = user.getParameter(paramName);

		assertThat(parameter.getName(), is(paramName));
		assertNull(parameter.getValue());
		assertThat(parameter.getDescription(),
				is("StratusLab account username"));
	}

}
