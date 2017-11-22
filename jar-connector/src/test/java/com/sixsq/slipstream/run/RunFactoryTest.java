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

import com.google.gson.JsonObject;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.*;
import com.sixsq.slipstream.factory.DeploymentFactory;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.util.CommonTestUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static com.sixsq.slipstream.util.SscljProxy.parseJson;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class RunFactoryTest extends RunTest {

	protected static ImageModule imageCircular1of2 = null;
	protected static ImageModule imageCircular2of2 = null;

	protected static ImageModule imageCircular1of3 = null;
	protected static ImageModule imageCircular2of3 = null;
	protected static ImageModule imageCircular3of3 = null;

	protected static ImageModule baseImage = null;
	protected static ImageModule customImage = null;

	private static String serviceOfferId = "service-offer/963f4e61-5017-4a00-be05-7dd275e5029e";
	private static JsonObject serviceOffer = null;

	@BeforeClass
	public static void setupClass() throws ValidationException {
		Event.muteForTests();
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
					.resetAndLoadConnector(LocalConnector.class);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		serviceOffer = parseJson(serviceOfferJson);
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

	private Run getBuildImageRun(ImageModule image) throws SlipStreamClientException {
		return RunFactory.getRun(image, RunType.Machine, user);
	}

	private Run getImageRun(ImageModule image) throws SlipStreamClientException {
		HashMap<String, List<Parameter<?>>> userChoices = new HashMap<>();

		Parameter<?> cloudService = new ModuleParameter(RuntimeParameter.CLOUD_SERVICE_NAME);
		cloudService.setValue(cloudServiceName);

		Parameter<?> serviceOffer = new ModuleParameter(RuntimeParameter.SERVICE_OFFER);
		serviceOffer.setValue(serviceOfferId);

		userChoices.put(Run.MACHINE_NAME, new ArrayList<>());
		userChoices.get(Run.MACHINE_NAME).add(cloudService);
		// userChoices.get(Run.MACHINE_NAME).add(serviceOffer);

		return RunFactory.getRun(image, RunType.Run, user, userChoices);
	}

	private Run getDeploymentRun(DeploymentModule deployment) throws SlipStreamClientException {
		HashMap<String, List<Parameter<?>>> userChoices = new HashMap<>();

		for (String nodeName: deployment.getNodes().keySet()) {
			Parameter<?> cloudService = new NodeParameter(RuntimeParameter.CLOUD_SERVICE_NAME);
			cloudService.setValue("'" + cloudServiceName + "'");

			Parameter<?> serviceOffer = new NodeParameter(RuntimeParameter.SERVICE_OFFER);
			serviceOffer.setValue("'" + serviceOfferId + "'");

			userChoices.put(nodeName, new ArrayList<>());
			userChoices.get(nodeName).add(cloudService);
			// userChoices.get(nodeName).add(serviceOffer);
		}

		return RunFactory.getRun(deployment, RunType.Orchestration, user, userChoices);
	}

	@Test(expected = ValidationException.class)
	public void cannotCreateDeploymentWithImageModule()
			throws SlipStreamClientException {
		RunFactory.getRun(image, RunType.Orchestration, user);
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


	public static String serviceOfferJson = "" +
			"  {\n" +
			"    \"connector\" : {\n" +
			"      \"href\" : \"exoscale-ch-gva\"\n" +
			"    },\n" +
			"    \"description\" : \"VM (standard) with 2 vCPU, 4096 MiB RAM, 200 GiB root disk, windows [CH] (Medium)\",\n" +
			"    \"price:currency\" : \"EUR\",\n" +
			"    \"resource:vcpu\" : 2,\n" +
			"    \"price:unitCost\" : 0.07597275199999999,\n" +
			"    \"resource:operatingSystem\" : \"windows\",\n" +
			"    \"updated\" : \"2017-07-13T03:00:12.031Z\",\n" +
			"    \"price:billingPeriodCode\" : \"MIN\",\n" +
			"    \"name\" : \"(2/4096/200 Medium windows) [CH]\",\n" +
			"    \"resource:class\" : \"standard\",\n" +
			"    \"created\" : \"2017-06-26T10:09:28.607Z\",\n" +
			"    \"price:billingUnitCode\" : \"HUR\",\n" +
			"    \"price:freeUnits\" : 0,\n" +
			"    \"price:unitCode\" : \"C62\",\n" +
			"    \"resource:instanceType\" : \"Medium\",\n" +
			"    \"exoscale:zone\" : \"ch-gva-2\",\n" +
			"    \"resource:diskType\" : \"SSD\",\n" +
			"    \"id\" : \"service-offer/35219a83-ee7f-41ac-b006-291d35504931\",\n" +
			"    \"schema-org:name\" : \"Medium\",\n" +
			"    \"resource:country\" : \"CH\",\n" +
			"    \"resource:platform\" : \"cloudstack\",\n" +
			"    \"resource:ram\" : 4096,\n" +
			"    \"acl\" : {\n" +
			"      \"owner\" : {\n" +
			"        \"type\" : \"ROLE\",\n" +
			"        \"principal\" : \"ADMIN\"\n" +
			"      },\n" +
			"      \"rules\" : [ {\n" +
			"        \"principal\" : \"USER\",\n" +
			"        \"right\" : \"VIEW\",\n" +
			"        \"type\" : \"ROLE\"\n" +
			"      }, {\n" +
			"        \"principal\" : \"ADMIN\",\n" +
			"        \"right\" : \"ALL\",\n" +
			"        \"type\" : \"ROLE\"\n" +
			"      } ]\n" +
			"    },\n" +
			"    \"resourceURI\" : \"http://sixsq.com/slipstream/1/ServiceOfferRef\",\n" +
			"    \"resource:disk\" : 200,\n" +
			"    \"resource:type\" : \"VM\"\n" +
			"  }";

}
