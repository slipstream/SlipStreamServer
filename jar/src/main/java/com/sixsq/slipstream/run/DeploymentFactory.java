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

public class DeploymentFactory extends RunFactory {

	@Override
	public Run createRun(Module module, String cloudService, User user)
			throws ValidationException, NotFoundException {

		DeploymentModule deployment = (DeploymentModule) module;

		Run run = constructRun(deployment, RunType.Orchestration, cloudService,
				user);

		DeploymentFactory.initOrchestratorsNodeNames(run);

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

			String nodename = constructNodeName(node.getName(), i);
			run.assignRuntimeParameters(nodename);
			run.assignRuntimeParameter(
					constructParamName(nodename,
							RuntimeParameter.MULTIPLICITY_PARAMETER_NAME),
					String.valueOf(multiplicity),
					"Multiplicity value for this node");
			String paramname = RuntimeParameter.NODE_NAME;
			run.assignRuntimeParameter(constructParamName(nodename, paramname),
					node.getName(), "Nodename");
			run.assignRuntimeParameter(
					constructParamName(nodename, RuntimeParameter.NODE_INDEX),
					String.valueOf(i), "Node index");

			run.assignRuntimeParameter(
					constructParamName(nodename,
							RuntimeParameter.CLOUD_SERVICE_NAME),
					cloudServiceName,
					RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);

			run.addNodeName(nodename);
		}

		run.addGroup(node.getName(), cloudServiceName);

		return run;
	}

	private static String constructNodeName(String groupname, int index) {
		return RuntimeParameter.constructNodeName(groupname, index);
	}

	private static String constructParamName(String nodename, String paramname) {
		return RuntimeParameter.constructParamName(nodename, paramname);
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
