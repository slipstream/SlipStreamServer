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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.InvalidMetadataException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.DeploymentFactory;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.util.CommonTestUtil;

public class RunFactoryTest extends RunTest {

	protected static ImageModule imageCircular1of2 = null;
	protected static ImageModule imageCircular2of2 = null;

	protected static ImageModule imageCircular1of3 = null;
	protected static ImageModule imageCircular2of3 = null;
	protected static ImageModule imageCircular3of3 = null;

	protected static ImageModule baseImage = null;
	protected static ImageModule customImage = null;

	@BeforeClass
	public static void setupClass() throws ValidationException {
		try {
			RunTest.setupClass();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		create2ElementCircularDependentImages();
		create3ElementCircularDependentImages();
		try {
			setupDeployments();
			createImage();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		try {
			CommonTestUtil
					.resetAndLoadConnector(com.sixsq.slipstream.connector.local.LocalConnector.class);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	private static void create2ElementCircularDependentImages()
			throws ValidationException {
		imageCircular1of2 = new ImageModule("test/imagecircular1of2");
		imageCircular2of2 = new ImageModule("test/imagecircular2of2");

		imageCircular1of2.setModuleReference(imageCircular2of2);
		imageCircular2of2.setModuleReference(imageCircular1of2);

		imageCircular1of2.store();
		imageCircular2of2.store();
	}

	private static void create3ElementCircularDependentImages()
			throws ValidationException {
		imageCircular1of3 = new ImageModule("test/imagecircular1of3");
		imageCircular2of3 = new ImageModule("test/imagecircular2of3");
		imageCircular3of3 = new ImageModule("test/imagecircular3of3");

		imageCircular1of3.setModuleReference(imageCircular2of3);
		imageCircular2of3.setModuleReference(imageCircular3of3);
		imageCircular3of3.setModuleReference(imageCircular1of3);

		imageCircular1of3.store();
		imageCircular2of3.store();
		imageCircular3of3.store();
	}

	private static void createImage() throws ValidationException {

		baseImage = new ImageModule("test/baseImage");
		baseImage.setImageId("123", cloudServiceName);
		baseImage.store();

		customImage = new ImageModule("test/customImage");
		customImage.setRecipe("echo hello;");
		customImage.setModuleReference(baseImage);
		customImage.store();
	}

	@AfterClass
	public static void teardownClass() {
		RunTest.teardownClass();

		if (imageCircular1of2 != null) {
			imageCircular1of2.remove();
		}
		if (imageCircular2of2 != null) {
			imageCircular2of2.remove();
		}
		if (imageCircular1of3 != null) {
			imageCircular1of3.remove();
		}
		if (imageCircular2of3 != null) {
			imageCircular2of3.remove();
		}
		if (imageCircular3of3 != null) {
			imageCircular3of3.remove();
		}

		if (deployment != null) {
			deployment.remove();
		}
	}

	@Test(expected = SlipStreamClientException.class)
	public void cannotBuildBaseImage() throws SlipStreamException {

		getBuildImageRun(imagebase);

	}

	@Test(expected = SlipStreamClientException.class)
	public void cannotBuildImageWithoutReference() throws SlipStreamException {

		getBuildImageRun(imagenoref);

	}

	@Test(expected = InvalidMetadataException.class)
	public void cannotBuildImageWith2ElementCircularDependency()
			throws SlipStreamClientException {

		getBuildImageRun(imageCircular1of2);

	}

	@Test(expected = InvalidMetadataException.class)
	public void cannotBuildImageWith3ElementCircularDependency()
			throws SlipStreamException {

		getBuildImageRun(imageCircular1of3);

	}

	private Run getBuildImageRun(ImageModule image)
			throws SlipStreamClientException {
		return RunFactory
				.getRun(image, RunType.Machine, cloudServiceName, user);
	}

	private Run getDeploymentRun(DeploymentModule deployment)
			throws SlipStreamClientException {
		return RunFactory.getRun(deployment, RunType.Orchestration,
				cloudServiceName, user);
	}

	@Test(expected = ValidationException.class)
	public void cannotCreateDeploymentWithImageModule()
			throws SlipStreamClientException {
		RunFactory.getRun(image, RunType.Orchestration, cloudServiceName, user);
	}

	@Test(expected = ValidationException.class)
	public void deploymentWithImageWithoutReference()
			throws SlipStreamClientException {
		DeploymentModule deploymentwithimagenoref = new DeploymentModule(
				"deploymentWithImageWithoutReference");
		Node node = new Node("node1", imagenoref);
		deploymentwithimagenoref.setNode(node);

		deploymentwithimagenoref.store();
		try {
			getDeploymentRun(deploymentwithimagenoref);
		} finally {
			deploymentwithimagenoref.remove();
		}
	}

	@Test
	public void deploymentRuntimeParameterInitialState()
			throws SlipStreamException {

		Run run = getDeploymentRun(deployment);

		assertThat(run.getRuntimeParameterValue("ss:state"),
				is("Initializing"));
		assertThat(run.getRuntimeParameterValue("node1.1:" + RuntimeParameter.IS_ORCHESTRATOR_KEY),
				is("false"));

	}

	@Test
	public void commonDeploymentRuntimeParameters() throws SlipStreamException {

		Run run = getDeploymentRun(deployment);

		String[] nodePrefixes = {
				Run.constructOrchestratorName(cloudServiceName)
						+ RuntimeParameter.NODE_PROPERTY_SEPARATOR, "node1.1:" };
		commonRuntimeParameters(run, nodePrefixes);

	}

	@Test
	public void commonImageRuntimeParameters() throws SlipStreamException {

		Run run = getBuildImageRun(customImage);
		String[] nodePrefixes = {
				Run.constructOrchestratorName(cloudServiceName) + RuntimeParameter.NODE_PROPERTY_SEPARATOR,
				Run.MACHINE_NAME_PREFIX };
		commonRuntimeParameters(run, nodePrefixes);

	}

	@Test
	public void outputParameterWithDefaultValueGetsMapped()
			throws SlipStreamException {

		ImageModule imageWithOutputParameter = new ImageModule(
				"test/imageWithOutputParameter");
		imageWithOutputParameter.setRecipe("echo hello;");
		imageWithOutputParameter.setImageId("123", cloudServiceName);
		ModuleParameter po1 = new ModuleParameter("po1", "po1 init value",
				"po1 desc", ParameterCategory.Output);
		imageWithOutputParameter.getParameters().put(po1.getName(), po1);
		imageWithOutputParameter.setModuleReference(baseImage);

		ImageModule imageWithInputParameter = new ImageModule(
				"test/imageWithInputParameter");
		imageWithInputParameter.setRecipe("echo hello;");
		imageWithInputParameter.setImageId("123", cloudServiceName);
		ModuleParameter pi1 = new ModuleParameter("pi1", "", "pi1 desc",
				ParameterCategory.Input);
		imageWithInputParameter.getParameters().put(pi1.getName(), pi1);
		imageWithInputParameter.setModuleReference(baseImage);

		DeploymentModule deployment = new DeploymentModule(
				"test/outputParameterWithDefaultValueGetsMappedDeployment");

		Node node;
		NodeParameter parameter;

		node = new Node("node1", imageWithOutputParameter);
		node.setCloudService(cloudServiceName);
		deployment.setNode(node);

		node = new Node("node2", imageWithInputParameter);
		node.setCloudService(cloudServiceName);

		parameter = new NodeParameter("pi1", "node1:po1", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
		deployment.setNode(node);

		deployment.store();

		Run run = getDeploymentRun(deployment);

		assertThat(run.getRuntimeParameterValue("node2.1:pi1"),
				is(po1.getValue()));

	}

	private void commonRuntimeParameters(Run run, String[] nodePrefixes)
			throws AbortException, NotFoundException {
		String[] keys = { RuntimeParameter.COMPLETE_KEY,
				RuntimeParameter.ABORT_KEY,	RuntimeParameter.IS_ORCHESTRATOR_KEY };

		for (String prefix : nodePrefixes) {
			for (String key : keys) {
				String fullKey = prefix + key;
				assertNotNull(run.getRuntimeParameterValue(fullKey),
						"Failed, key not set: " + fullKey);
			}
		}
	}

	@Test
	public void validDeploymentParameterMapping() throws ValidationException {
		Node node;
		NodeParameter parameter;

		node = new Node("node1", imageForDeployment1);
		node.setCloudService(cloudServiceName);
		parameter = new NodeParameter("pi1", "node2:po2", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
		deployment.setNode(node);

		node = new Node("node2", imageForDeployment2);
		node.setCloudService(cloudServiceName);
		parameter = new NodeParameter("pi2", "node1:po1", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
		deployment.setNode(node);

	}

	@Test
	public void validDeploymentParameterMappingSettingStringInsteadOfMapping()
			throws ValidationException {
		Node node;
		NodeParameter parameter;

		node = new Node("node1", imageForDeployment1);
		parameter = new NodeParameter("pi1", "\"123\"", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
	}

	@Test(expected = ValidationException.class)
	public void invalidDeploymentParameterMappingWithOutputToOutput()
			throws ValidationException {
		Node node;
		NodeParameter parameter;

		node = new Node("node1", imageForDeployment1);
		parameter = new NodeParameter("po1", "node2:po2", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
	}

	@Test(expected = ValidationException.class)
	public void invalidDeploymentParameterMappingWithInputToInput()
			throws ValidationException {
		Node node;
		NodeParameter parameter;

		node = new Node("node1", imageForDeployment1);
		parameter = new NodeParameter("pi1", "node2:pi2", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
	}

	@Test
	public void nodeNames() throws SlipStreamException {

		Run run = getDeploymentRun(deployment);

		int ORCHESTRATOR_AND_2_NODES = 3;
		assertThat(run.getNodeNames().split(",").length,
				is(ORCHESTRATOR_AND_2_NODES));
	}

	@Test
	public void runContainsInputParametersAsRuntimeParameters()
			throws SlipStreamException {
		Node node;
		NodeParameter parameter;

		node = new Node("node1", imageForDeployment1);
		parameter = new NodeParameter("pi1", "\"astring\"", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
		node.setCloudService(cloudServiceName);

		deployment.setNode(node);

		Run run = getDeploymentRun(deployment);

		assertTrue(run.getRuntimeParameters().containsKey("node1.1:pi1"));
	}

	@Test
	public void runContainsMapping() throws SlipStreamException {

		Node node;
		NodeParameter parameter;

		node = new Node("node1", imageForDeployment1);
		node.setCloudService(cloudServiceName);
		parameter = new NodeParameter("pi1", "node2:po2", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
		deployment.setNode(node);

		node = new Node("node2", imageForDeployment2);
		node.setCloudService(cloudServiceName);
		parameter = new NodeParameter("pi2", "\"astring\"", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
		deployment.setNode(node);

		Run run = getDeploymentRun(deployment);

		assertTrue(run.getRuntimeParameters().get("node2.1:po2").isMapsOthers());
		assertThat(run.getRuntimeParameters().get("node2.1:po2")
				.getMappedRuntimeParameterNames(), is("node1.1:pi1,"));
	}

	@Test
	public void runtimeParameterSetAlsoSetsMappedRuntimeParameters()
			throws SlipStreamException {

		Node node;
		NodeParameter parameter;

		node = new Node("node1", imageForDeployment1);
		node.setCloudService(cloudServiceName);
		parameter = new NodeParameter("pi1", "node2:po2", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
		deployment.setNode(node);

		node = new Node("node2", imageForDeployment2);
		node.setCloudService(cloudServiceName);
		parameter = new NodeParameter("pi2", "\"astring\"", null);
		parameter.setContainer(node);
		node.setParameterMapping(parameter, deployment);
		deployment.setNode(node);

		Run run = getDeploymentRun(deployment);
		run.store();

		run.getRuntimeParameters().get("node2.1:po2").setValue("new value");

		assertThat(run.getRuntimeParameters().get("node2.1:po2").getValue(),
				is("new value"));
		assertThat(run.getRuntimeParameters().get("node1.1:pi1").getValue(),
				is("new value"));
	}

	@Test
	public void insertMultiplicityIndexInName() {
		assertThat(DeploymentFactory.insertMultiplicityIndexInParameterName(
				"node:param", 1), is("node.1:param"));
	}
}
