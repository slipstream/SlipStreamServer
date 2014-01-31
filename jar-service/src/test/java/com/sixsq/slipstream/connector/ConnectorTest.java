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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.junit.Test;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.CommonTestUtil;

public class ConnectorTest extends ConnectorDummy {

	protected static final String INSTANCE_NAME = "local";

	public ConnectorTest() {
		super(INSTANCE_NAME);
	}

	@Test
	public void updateInstanceIdAndIpOnRunTest()
			throws ServerExecutionEnginePluginException, AbortException,
			SlipStreamClientException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {
		User user = CommonTestUtil.createTestUser();
		DeploymentModule deployment = CommonTestUtil.createDeployment();

		CommonTestUtil.addSshKeys(user);

		CommonTestUtil.resetAndLoadConnector(
				com.sixsq.slipstream.connector.local.LocalConnector.class,
				INSTANCE_NAME);

		Run run = RunFactory.getRun(deployment, RunType.Orchestration,
				INSTANCE_NAME, user);

		run = run.store();
		
		updateInstanceIdAndIpOnRun(run, "foo", "bar");

		RuntimeParameter instanceId = RuntimeParameter.loadFromUuidAndKey(run.getUuid(), Run
				.constructOrchestratorName(INSTANCE_NAME)
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
				+ RuntimeParameter.INSTANCE_ID_KEY);
		
		assertThat(instanceId.getValue(), is("foo"));

		RuntimeParameter hostname = RuntimeParameter.loadFromUuidAndKey(run.getUuid(), Run
				.constructOrchestratorName(INSTANCE_NAME)
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
				+ RuntimeParameter.HOSTNAME_KEY);
		
		assertThat(hostname.getValue(), is("bar"));

		CommonTestUtil.deleteUser(user);
	}

	@Test
	public void getExtraDisksTest() {
		assertEquals(Collections.emptyList(), getExtraDisks());
	}

	@Test
	public void validateExtraDiskParameterTest() {
		try {
			validateExtraDiskParameter("foo", null);
		} catch (Exception e) {
			fail("Providing 'null' parmater value shouldn't throw. " + e);
		}

		try {
			validateExtraDiskParameter("foo", "");
		} catch (ValidationException e) {
			fail("Providing empty parmater value shouldn't throw.");
		}

		defineExtraDisk("bar", "blah", "A", "");
		try {
			validateExtraDiskParameter("bar", "a");
			fail("Should have failed.");
		} catch (ValidationException e) {
		}

		defineExtraDisk("baz", "blah", "^a$", "");
		try {
			validateExtraDiskParameter("baz", "a");
		} catch (ValidationException e) {
			fail("Shouldn't have failed.");
		}
	}
}
