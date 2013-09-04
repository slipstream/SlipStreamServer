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

import java.util.HashSet;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;

public class RunDeploymentFactory extends RunFactory {

	@Override
	public Run createRun(Module module, RunType type, String cloudService,
			User user) throws ValidationException, NotFoundException {

		DeploymentModule deployment = (DeploymentModule) module;

		Run run = constructRun(deployment, cloudService, user);

		RunDeploymentFactory.initOrchestratorsNodeNames(run);

		run = initRuntimeParameters(deployment, run);
		run = initRuntimeParametersMapping(deployment, run);

		validate(deployment, run);

		return run;
	}

	private void validate(DeploymentModule deployment, Run run)
			throws ValidationException {
		checkAllImagesHaveReferenceOrImageId(deployment, run);
	}

	private void checkAllImagesHaveReferenceOrImageId(
			DeploymentModule deployment, Run run) throws ValidationException {
		for (Node node : deployment.getNodes().values()) {
			String cloudServiceName = run.getEffectiveCloudServiceName(node
					.getCloudService());
			try {
				checkImageHasReferenceOrImageId(node.getImage(),
						cloudServiceName);
			} catch (ValidationException ex) {
				throw new ValidationException("Node " + node.getName()
						+ " refers to image " + ex.getMessage());
			}
		}
	}

	private void checkImageHasReferenceOrImageId(ImageModule image,
			String cloudServiceName) throws ValidationException {

		if (!"".equals(image.getCloudImageId(cloudServiceName))) {
			return;
		}
		boolean mustHaveImageId = image.isBase() || !image.isVirtual();
		if (mustHaveImageId
				&& "".equals(image.getCloudImageId(cloudServiceName))) {
			throw new ValidationException(image.getName()
					+ " missing an image id for cloud: " + cloudServiceName
					+ ". Did you build it?");
		} else if ("".equals(image.getModuleReference())) {
			throw new ValidationException(image.getName()
					+ " missing a machine image reference");
		} else {
			String referenceUri = image.getModuleReference();
			ImageModule reference = (ImageModule) ImageModule
					.load(referenceUri);
			if (reference == null) {
				throw new ValidationException("Image " + image.getName()
						+ " refers to an unknown image "
						+ image.getModuleReference());
			}
			checkImageHasReferenceOrImageId(reference, cloudServiceName);
		}
	}

	private static Run initRuntimeParameters(DeploymentModule deployment,
			Run run) throws ValidationException, NotFoundException {

		for (Node node : deployment.getNodes().values()) {
			run = initMachineState(node, run);
			Module image = node.getImage();
			for (ModuleParameter param : image.getParameterList()) {
				String initialValue = extractInitialValue(param, node);
				run.createRuntimeParameter(node, param.getName(), initialValue,
						param.getDescription(), param.getType());
			}
		}
		return run;
	}

	private static Run initRuntimeParametersMapping(
			DeploymentModule deployment, Run run) throws ValidationException,
			NotFoundException {

		for (Node node : deployment.getNodes().values()) {
			int multiplicity = node.getMultiplicity();
			for (NodeParameter param : node.getParameterMappings().values()) {
				for (int i = 1; i <= multiplicity; i++) {
					if (!param.isStringValue()) {
						addParameterMapping(run, param, i);
					}
				}
			}
		}

		return run;
	}

	private static void addParameterMapping(Run run, NodeParameter param, int i) {
		String name = insertMultiplicityIndexInParameterName(param.getValue(),
				1);
		RuntimeParameter input = run.getRuntimeParameters().get(name);
		input.setMapsOthers(true);
		input.addMappedRuntimeParameterName(insertMultiplicityIndexInParameterName(
				param.getContainer().getName()
						+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
						+ param.getName(), i));
		if (input.isSet()) {
			input.setValue(input.getValue());
		}
		run.getRuntimeParameters().put(input.getName(), input);
	}

	protected static String insertMultiplicityIndexInParameterName(String name,
			int index) {
		String[] parts = name.split(RuntimeParameter.NODE_PROPERTY_SEPARATOR);
		return parts[0] + RuntimeParameter.NODE_MULTIPLICITY_INDEX_SEPARATOR
				+ index + RuntimeParameter.NODE_PROPERTY_SEPARATOR + parts[1];
	}

	private static Run initMachineState(Node node, Run run)
			throws ValidationException, NotFoundException {

		int multiplicity = node.getMultiplicity();
		String cloudServiceName = run.getEffectiveCloudServiceName(node
				.getCloudService());

		for (int i = 1; i <= multiplicity; i++) {

			addNodeInstanceRuntimeParameters(node, run, multiplicity,
					cloudServiceName, i);
		}

		run.addGroup(node.getName(), cloudServiceName);

		return run;
	}

	public static void addNodeInstance(Run run, String nodeName, int variation)
			throws ValidationException, NotFoundException, AbortException {
		Node node = run.getNodes().get(nodeName);
		int oldMultiplicity = node.getMultiplicity();
		int newMultiplicity = oldMultiplicity + variation;
		if (variation > 0) {
			for (int i = oldMultiplicity + 1; i <= newMultiplicity; i++) {
				addNodeInstance(run, node, i, newMultiplicity);
				updateMultiplicityForAll(run, node, newMultiplicity);
			}
		} else if (variation < 0) {
			// todo...
		}
	}

	private static void updateMultiplicityForAll(Run run, Node node,
			int newMultiplicity) throws ValidationException {
		node.setMultiplicity(newMultiplicity);
		for (int i = RuntimeParameter.MULTIPLICITY_NODE_START_INDEX; i < newMultiplicity; i++) {
			run.getRuntimeParameters().get(multiplicityKey(node.getName(), i))
					.setValue(String.valueOf(newMultiplicity));
		}
	}

	public static Run addNodeInstance(Run run, Node node, int index,
			int multiplicity) throws NotFoundException, AbortException,
			ValidationException {
		addNodeInstanceRuntimeParameters(node, run, multiplicity,
				node.getCloudService(), index);
		return run;
	}

	protected static void addNodeInstanceRuntimeParameters(Node node, Run run,
			int multiplicity, String cloudServiceName, int index)
			throws ValidationException {
		String nodeNamePartWithNodePropertySeparator = constructNodeNamePartWithNodePropertySeparator(
				node.getName(), index);

		run.assignRuntimeParameters(nodeNamePartWithNodePropertySeparator);
		run.assignRuntimeParameter(nodeNamePartWithNodePropertySeparator
				+ RuntimeParameter.MULTIPLICITY_PARAMETER_NAME,
				String.valueOf(multiplicity),
				"Multiplicity value for this node");
		run.assignRuntimeParameter(nodeNamePartWithNodePropertySeparator
				+ RuntimeParameter.NODE_NAME, node.getName(), "Nodename");
		run.assignRuntimeParameter(nodeNamePartWithNodePropertySeparator
				+ RuntimeParameter.NODE_INDEX, String.valueOf(index),
				"Node index");

		run.assignRuntimeParameter(cloudServiceNameKey(node.getName(), index),
				cloudServiceName, RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);

		run.addNodeName(constructNodeNamePart(node.getName(), index));
	}

	protected static String cloudServiceNameKey(String node, int index) {
		return constructNodeNamePartWithNodePropertySeparator(node, index)
				+ RuntimeParameter.CLOUD_SERVICE_NAME;
	}

	protected static String multiplicityKey(String node, int index) {
		return constructNodeNamePartWithNodePropertySeparator(node, index)
				+ RuntimeParameter.MULTIPLICITY_PARAMETER_NAME;
	}

	private static String constructNodeNamePart(String node, int index) {
		return node + RuntimeParameter.NODE_MULTIPLICITY_INDEX_SEPARATOR
				+ index;
	}

	private static String constructNodeNamePartWithNodePropertySeparator(
			String node, int index) {
		return constructNodeNamePart(node, index)
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR;
	}

	private static String extractInitialValue(ModuleParameter parameter,
			Node node) {

		String initialNodeValue = extractInitialValue(node
				.getParameter(parameter.getName()));
		String defaultModuleParameter = parameter.getValue();

		return initialNodeValue == null ? defaultModuleParameter
				: initialNodeValue;
	}

	private static String extractInitialValue(NodeParameter parameter) {

		if (parameter == null) {
			return null;
		}

		String value = "";
		if (parameter.isStringValue()) {
			int length = parameter.getValue().length();
			value = parameter.getValue().substring(1, length - 1);
		}

		return value;
	}

	protected static void initOrchestratorsNodeNames(Run run)
			throws ConfigurationException, ValidationException {
		HashSet<String> cloudServiceList = run.getCloudServicesList();
		for (String cloudServiceName : cloudServiceList) {
			String nodename = Run.ORCHESTRATOR_NAME + "-" + cloudServiceName;
			run.addNodeName(nodename);
			run.assignRuntimeParameter(nodename
					+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
					+ RuntimeParameter.CLOUD_SERVICE_NAME, cloudServiceName,
					RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);

		}
	}

	@Override
	public Module overloadModule(Run run, User user) throws ValidationException {
		Module module = loadModule(run);
		return DeploymentModule.populateFromRun(run, module, user);
	}

}
