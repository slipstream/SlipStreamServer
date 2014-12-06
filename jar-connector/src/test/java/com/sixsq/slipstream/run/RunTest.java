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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Metadata;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunParameter;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.statemachine.States;
import com.sixsq.slipstream.util.CommonTestUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.Terminator;

public class RunTest extends RunTestBase {

	@BeforeClass
	public static void setupClass() throws ValidationException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {
		setupImages();
		CommonTestUtil
				.resetAndLoadConnector(com.sixsq.slipstream.connector.local.LocalConnector.class);
	}

	@AfterClass
	public static void teardownClass() {
		tearDownImages();
	}

	@Test
	public void verifyCorrectParameters() throws FileNotFoundException,
			IOException, SlipStreamException {

		Run run = RunFactory.getRun(image, RunType.Run, user);

		assertEquals(image.getResourceUri(), run.getModuleResourceUrl());
		assertEquals(ModuleCategory.Image, run.getCategory());
		assertNotNull(run.getResourceUri());
		assertNotNull(run.getUuid());
	}

	@Test
	public void storeRetrieveAndDelete() throws FileNotFoundException,
			IOException, SlipStreamException {

		Run run = RunFactory.getRun(image, RunType.Run, user);
		run.store();

		String resourceUrl = run.getResourceUri();

		Run runRestored = Run.load(resourceUrl);
		assertNotNull(runRestored);

		assertEquals(run.getResourceUri(), runRestored.getResourceUri());
		assertEquals(run.getModuleResourceUrl(),
				runRestored.getModuleResourceUrl());
		assertEquals(run.getCategory(), runRestored.getCategory());

		run.remove();
		runRestored = Run.load(resourceUrl);
		assertNull(runRestored);
	}

	@Test
	public void runWithOneParameter() throws FileNotFoundException,
			IOException, SlipStreamException {

		Run run = RunFactory.getRun(image, RunType.Run, user);

		int initialNumberOfParameters = run.getParameters().size();

		String resourceUrl = run.getResourceUri();

		String parameterName = "name";
		String description = "description";
		String value = "value";

		RunParameter parameter = new RunParameter(parameterName, value,
				description);
		run.setParameter(parameter);

		run.store();

		Run runRestored = Run.load(resourceUrl);
		assertNotNull(runRestored);

		Map<String, RunParameter> parameters = runRestored.getParameters();
		assertNotNull(parameters);
		assertEquals(1 + initialNumberOfParameters, parameters.size());

		parameter = parameters.get(parameterName);
		assertNotNull(parameter);
		assertEquals(parameterName, parameter.getName());
		assertEquals(description, parameter.getDescription());
		assertEquals(value, parameter.getValue());

		run.remove();
		run = Run.load(resourceUrl);
		assertNull(run);
	}

	@Test
	public void runWithTwoParameters() throws FileNotFoundException,
			IOException, SlipStreamException {

		Run run = RunFactory.getRun(image, RunType.Run, user);

		int initialNumberOfParameters = run.getParameters().size();

		String resourceUrl = run.getResourceUri();

		String parameterName1 = "p1";
		String description1 = "d1";
		String value1 = "v1";

		RunParameter parameter = new RunParameter(parameterName1, value1,
				description1);
		run.setParameter(parameter);

		String parameterName2 = "p2";
		String description2 = "d2";
		String value2 = "v2";

		parameter = new RunParameter(parameterName2, value2, description2);
		run.setParameter(parameter);

		run.store();

		Run runRestored = Run.load(resourceUrl);
		assertNotNull(runRestored);

		Map<String, RunParameter> parameters = runRestored.getParameters();
		assertNotNull(parameters);
		assertEquals(2 + initialNumberOfParameters, parameters.size());

		parameter = parameters.get(parameterName1);
		assertNotNull(parameter);
		assertEquals(parameterName1, parameter.getName());
		assertEquals(description1, parameter.getDescription());
		assertEquals(value1, parameter.getValue());

		parameter = parameters.get(parameterName2);
		assertNotNull(parameter);
		assertEquals(parameterName2, parameter.getName());
		assertEquals(description2, parameter.getDescription());
		assertEquals(value2, parameter.getValue());

		run.remove();
		run = Run.load(resourceUrl);
		assertNull(run);
	}

	@Test
	public void assignRuntimeParameters() throws FileNotFoundException,
			IOException, SlipStreamException {

		Run run = RunFactory.getRun(image, RunType.Run, user);

		int initialParameterNo = run.getRuntimeParameters().size();

		String resourceUrl = run.getResourceUri();

		String k1 = "node.1:k1";
		String v1 = "v1";
		String v1Desc = "v1 desc";

		run.assignRuntimeParameter(k1, v1, v1Desc);

		String k2 = "node.2:k2";
		String v2 = "v2";
		String v2Desc = "v2 desc";

		run.assignRuntimeParameter(k2, v2, v2Desc);

		run.store();

		Map<String, String> data = new HashMap<String, String>();
		data.put(k1, v1);
		data.put(k2, v2);

		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();

		Run runRestored = em.merge(Run.load(resourceUrl));
		assertNotNull(runRestored);

		Map<String, RuntimeParameter> parameters = runRestored
				.getRuntimeParameters();

		assertNotNull(parameters);
		assertEquals(initialParameterNo + data.size(), parameters.size());

		transaction.commit();
		em.close();

		assertEquals(v1, parameters.get(k1).getValue());
		assertEquals(v1Desc, parameters.get(k1).getDescription());

		assertEquals(v2, parameters.get(k2).getValue());
		assertEquals(v2Desc, parameters.get(k2).getDescription());

		run.remove();
		run = Run.load(resourceUrl);
		assertNull(run);

	}

	@Test
	public void createAndRetreive() throws FileNotFoundException, IOException,
			SlipStreamException {

		Run run = new Run(new ImageModule("createAndRetreive"), RunType.Orchestration, cloudServiceNames, user);
		run.store();

		Run runRestored = Run.loadFromUuid(run.getUuid());

		assertNotNull(runRestored);
		assertEquals(run.getUuid(), runRestored.getUuid());

		run.remove();
	}

	@Test
	public void viewListByModule() throws FileNotFoundException, IOException,
			SlipStreamException {

		Authz authz;
		ImageModule findit = new ImageModule("findit");
		authz = new Authz("test", findit);
		findit.setAuthz(authz);
		findit.setModuleReference(image);
		findit.setRecipe("a recipe");
		findit.store();
		Metadata run1 = createAndStoreRun(findit, RunType.Machine);
		Metadata run2 = createAndStoreRun(findit, RunType.Machine);

		ImageModule dontfindit = new ImageModule("dontfindit");
		dontfindit.setModuleReference(image.getResourceUri());
		dontfindit.setRecipe("a recipe");
		authz = new Authz("test", dontfindit);
		dontfindit.setAuthz(authz);
		dontfindit.store();
		Metadata run3 = createAndStoreRun(dontfindit, RunType.Machine);

		List<RunView> runList = Run.viewList(new User("user"), findit.getResourceUri());

		assertEquals(2, runList.size());

		run1.remove();
		run2.remove();
		run3.remove();

		findit.remove();
		dontfindit.remove();
	}

	@Test
	public void onlyViewListMyRuns() throws FileNotFoundException, IOException,
			SlipStreamException {

		Authz authz;
		ImageModule image = new ImageModule("onlyViewListMyRuns");
		authz = new Authz("test", image);
		image.setAuthz(authz);
		image.setModuleReference(RunTestBase.image);
		image.setRecipe("a recipe");
		image.store();
		Run myRun = createAndStoreRun(image, RunType.Machine);
		Run myOtherRun = createAndStoreRun(image, RunType.Machine);
		Run notMyRun = createAndStoreRun(image, "other", RunType.Machine);

		List<RunView> runList = Run.viewList(new User("user"), image.getResourceUri());

		assertEquals(2, runList.size());

		myRun.remove();
		myOtherRun.remove();
		notMyRun.remove();

		image.remove();
	}

	@Test
	public void checkRunSerialization() throws FileNotFoundException,
			IOException, SlipStreamException {

		Run run = RunFactory.getRun(image, RunType.Run, user);

		SerializationUtil.toXmlString(run);
	}

	@Test
	public void testParameters() throws SlipStreamException,
			FileNotFoundException, IOException {

		Run run = RunFactory.getRun(image, RunType.Run, user);

		run.setParameter(new RunParameter("k1", "v1", ""));
		run.setParameter(new RunParameter("k2", "v2", ""));

		assertNotSame(0, run.getParameters().size());

		RunParameter p;
		p = run.getParameter("k1");
		assertEquals("k1", p.getName());
		assertEquals("v1", p.getValue());

		p = run.getParameter("k2");
		assertEquals("k2", p.getName());
		assertEquals("v2", p.getValue());
	}

	@Test
	public void updateRuntimeParameters() throws SlipStreamException,
			FileNotFoundException, IOException {

		Run run = RunFactory.getRun(image, RunType.Run, user);

		run.assignRuntimeParameter("node.1:k1", "v1 init", "v1 desc");
		run.assignRuntimeParameter("node.1:k2", "v2 init", "v2 desc");

		run.updateRuntimeParameter("node.1:k1", "v1");
		run.updateRuntimeParameter("node.1:k2", "v2");

		assertEquals("v1", run.getRuntimeParameterValue("node.1:k1"));
		assertEquals("v2", run.getRuntimeParameterValue("node.1:k2"));
	}

	@Test(expected = NotFoundException.class)
	public void setInexistantRuntimeParameter() throws SlipStreamException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {
		CommonTestUtil
				.resetAndLoadConnector(com.sixsq.slipstream.connector.local.LocalConnector.class);

		ImageModule image = new ImageModule(
				"setInexistantRuntimeParameterImage");
		image.setImageId("123", cloudServiceName);
		ModuleParameter moduleParameter;

		moduleParameter = new ModuleParameter("pi1", null, "");
		moduleParameter.setCategory(ParameterCategory.Input);
		image.setParameter(moduleParameter);

		moduleParameter = new ModuleParameter("po1", null, "");
		moduleParameter.setCategory(ParameterCategory.Output);
		image.setParameter(moduleParameter);
		image = image.store();

		DeploymentModule deployment = new DeploymentModule(
				"setInexistantRuntimeParameter");

		NodeParameter parameter;

		Node node1 = new Node("node1", image);
		node1.setCloudService(cloudServiceName);
		deployment.setNode(node1);
		Node node2 = new Node("node2", image);
		node2.setCloudService(cloudServiceName);
		deployment.setNode(node2);

		parameter = new NodeParameter("pi1", "node2:po1", null);
		parameter.setContainer(node1);
		node1.setParameterMapping(parameter, deployment);

		parameter = new NodeParameter("pi1", "node1:po1", null);
		parameter.setContainer(node2);
		node2.setParameterMapping(parameter, deployment);

		deployment = deployment.store();

		Run run = RunFactory.getRun(deployment, RunType.Orchestration, user);

		try {
			run.updateRuntimeParameter("node1.1:notthere", null);
		} finally {
			deployment.remove();
			image.remove();
		}
	}

	@Test
	public void abort() throws ValidationException, ConfigurationException {

		ImageModule image = new ImageModule("abort");

		Run run = new Run(image, RunType.Orchestration, cloudServiceNames, user);

		assertThat(run.isAbort(), is(false));

		run.getRuntimeParameters().put(
				RuntimeParameter.GLOBAL_ABORT_KEY,
				new RuntimeParameter(run, RuntimeParameter.GLOBAL_ABORT_KEY,
						"Kaboom", ""));

		assertThat(run.isAbort(), is(true));

		RuntimeParameter p = run.getRuntimeParameters().get(
				RuntimeParameter.GLOBAL_ABORT_KEY);
		p.setIsSet(false);

		assertThat(run.isAbort(), is(false));
	}

	private Run setRunState(Run run, States state){
		EntityManager em = PersistenceUtil.createEntityManager();
		em.getTransaction().begin();
		run = Run.load(run.getResourceUri(), em);

		RuntimeParameter globalState = run.getRuntimeParameters().get(RuntimeParameter.GLOBAL_STATE_KEY);
		globalState.setValue(state.toString());
		globalState.store();

		run.setState(state);
		run.store();

		em.getTransaction().commit();
		em.close();

		return run;
	}

	@Test
	public void purge() throws ConfigurationException, SlipStreamException {

		ImageModule image = new ImageModule("doneImage");
		image.setImageId("123", cloudServiceName);

		Run run = RunFactory.getRun(image, RunType.Run, user);
		run.store();
		String resourceUri = run.getResourceUri();

		run = setRunState(run, States.Initializing);
		Terminator.purgeRun(run);
		run = Run.load(resourceUri);
		assertThat(run.getState(), is(States.Cancelled));

		run = setRunState(run, States.Executing);
		Terminator.purgeRun(run);
		run = Run.load(resourceUri);
		assertThat(run.getState(), is(States.Cancelled));

		run = setRunState(run, States.Ready);
		Terminator.purgeRun(run);
		run = Run.load(resourceUri);
		assertThat(run.getState(), is(States.Done));

		run = setRunState(run, States.Ready);
		run.getRuntimeParameters().put(
				RuntimeParameter.GLOBAL_ABORT_KEY,
				new RuntimeParameter(run, RuntimeParameter.GLOBAL_ABORT_KEY,
						"Kaboom", ""));
		run.store();
//		setRuntimeParameterState(run, RuntimeParameter.GLOBAL_STATE_KEY, States.Aborting);
		Terminator.purgeRun(run);
		run = Run.load(resourceUri);
		assertThat(run.getState(), is(States.Aborted));

		run.remove();
	}

}
