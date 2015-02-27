package com.sixsq.slipstream.connector;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2014 SixSq Sarl (sixsq.com)
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
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.connector.local.LocalUserParametersFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.run.RunTestBase;


public class CollectorTest extends RunTestBase {

	@BeforeClass
	public static void setupClass() throws ConfigurationException, SlipStreamException {
		createUser();
		for(Run r : Run.listAll()) {
			r.remove();
		}
	}

	protected static void createUser() throws ConfigurationException, ValidationException {
		RunTestBase.createUser();
		LocalUserParametersFactory lpf = new LocalUserParametersFactory(localConnector.getConnectorInstanceName());
		String key = lpf.constructKey(LocalUserParametersFactory.KEY_PARAMETER_NAME);
		user.setParameter(new UserParameter(key, "key value", ""));
		String secret = lpf.constructKey(LocalUserParametersFactory.SECRET_PARAMETER_NAME);
		user.setParameter(new UserParameter(secret, "secret value", ""));
	}

	@Test
	public void collect() {
		int res = Collector.collect(user, localConnector, 0);
		assertThat(res,  is(LocalConnector.MAX_VMS));
	}
}
