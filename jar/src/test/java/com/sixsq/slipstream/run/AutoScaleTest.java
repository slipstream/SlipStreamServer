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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sixsq.slipstream.common.util.CommonTestUtil;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidMetadataException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunTest;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;

public class AutoScaleTest extends RunFactoryTest {

	@BeforeClass
	public static void setupClass() throws ValidationException {
		RunFactoryTest.setupClass();
	}

	@Before
	public void setup() {
	}

	@AfterClass
	public static void teardownClass() {
		RunFactoryTest.teardownClass();
	}
	
	@After
	public void tearDown() throws ValidationException {
	}
	
	@Test
	public void addNodeInstanceToRun() throws SlipStreamClientException, AbortException {
		Run run = RunFactory.getRun(deployment, cloudServiceName, user);
		
		String nodeName = "node1";
		String multiplicityParameterName = RuntimeParameter.composeName(nodeName, 1, RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);
		assertThat(run.getRuntimeParameterValue(multiplicityParameterName), is("1"));
		RunDeploymentFactory.addNodeInstance(run, "node1", 1);
		assertThat(run.getRuntimeParameterValue(multiplicityParameterName), is("2"));
	}
}
