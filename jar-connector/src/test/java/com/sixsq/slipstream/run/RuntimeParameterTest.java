package com.sixsq.slipstream.run;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.Metadata;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.CommonTestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class RuntimeParameterTest {

	private static User user;

	@BeforeClass
	public static void setupClass() throws ValidationException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {
		user = CommonTestUtil.createTestUser();

		CommonTestUtil.addSshKeys(user);

		CommonTestUtil
				.resetAndLoadConnector(com.sixsq.slipstream.connector.local.LocalConnector.class);
	}

	@AfterClass
	public static void teardownClass() {
		CommonTestUtil.deleteUser(user);
	}

	@Test(expected = ValidationException.class)
	public void nullRun() throws SlipStreamException {

		new RuntimeParameter(null, "node.1:dummy", "ok", "ok");

	}

	@Test(expected = ValidationException.class)
	public void nullParameterName() throws SlipStreamException {
		DeploymentModule deployment = CommonTestUtil.createDeployment();

		Run run = RunFactory.getRun(deployment, RunType.Orchestration, user);
		run.assignRuntimeParameter(null, "ok", null);

		CommonTestUtil.deleteDeployment(deployment);
	}

	@Test
	public void serializationWorks() throws SlipStreamException {
		DeploymentModule deployment = CommonTestUtil.createDeployment();

		Run run = RunFactory.getRun(deployment, RunType.Orchestration, user);
		Metadata parameter = run.assignRuntimeParameter("node.1:dummy", "ok",
				null);

		String serialization = SerializationUtil.toXmlString(parameter);

		assertNotNull(serialization);
		assertFalse("".equals(serialization));

		Document document = SerializationUtil.toXmlDocument(parameter);

		assertNotNull(document);

		CommonTestUtil.deleteDeployment(deployment);
	}

	@Test
	public void checkValidNames() throws SlipStreamException {
		DeploymentModule deployment = CommonTestUtil.createDeployment();

		String[] validNames = { "ss:a", "orchestrator:a", "machine:a",
				"host.1:a", "host1.2:a", "ss.1:a", "orchestrator.1:a",
				"orchestrator-with-da-shes:a", "machine.1:a",
				"host1.2:with.dots", "host1.2:with-dash",
				"host1.2:with_underscore" };

		Run run = RunFactory.getRun(deployment, RunType.Orchestration, user);
		for (String name : validNames) {
			new RuntimeParameter(run, name, "", "");
		}

		CommonTestUtil.deleteDeployment(deployment);
	}

	@Test
	public void checkInvalidNames() throws ValidationException {
		String[] invalidNames = { "", "a", ":", ":a", " :a", ".:a", "a.b:a",
				"ssx:a", "a:b:c", "a:b.c", "a:1", "ss:", "orchestrator:",
				"orchestrator--a:a", "machine:", "missing_key.1:",
				"missing_key:", "name-with-dashes.1:a" };

		for (String name : invalidNames) {
			try {
				new RuntimeParameter(null, name, "ok", "ok");
				fail("invalid RuntimeParameter name did not throw an exception: "
						+ name);
			} catch (ValidationException e) {
				// OK.
			}
		}
	}

	@Test
	public void extractParameterParts() {
		assertThat(RuntimeParameter.extractNodeNamePart("node:param"),
				is("node"));
		assertNull(RuntimeParameter.extractNodeNamePart("something"));

		assertThat(RuntimeParameter.extractParamNamePart("node:param"),
				is("param"));
		assertNull(RuntimeParameter.extractParamNamePart("something"));
	}


	@Test
	public void isInitiallyNotSet() throws SlipStreamClientException {
		DeploymentModule deployment = CommonTestUtil.createDeployment();

		Run run = RunFactory.getRun(deployment, RunType.Orchestration, user);
		RuntimeParameter rp = run.getRuntimeParameters().get("ss:complete");
		assertFalse(rp.isSet());
	}

}
