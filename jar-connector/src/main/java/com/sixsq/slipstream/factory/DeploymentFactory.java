package com.sixsq.slipstream.factory;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunParameter;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;

public class DeploymentFactory extends RunFactory {

	@Override
	protected Run constructRun(Module module, String cloudService, User user)
			throws ValidationException {
		Run run = new Run(module, RunType.Orchestration, cloudService, user);
		return run;
	}

	@Override
	protected void init(Module module, Run run, User user, String cloudService)
			throws ValidationException, NotFoundException {

		initNodeRunParameters(run);
		initNodesInstancesRuntimeParameters(run);
		initNodesRuntimeParameters(run);
	}

	@Override
	protected void validateRun(Run run, User user, String cloudService)
			throws SlipStreamClientException {

		super.validateRun(run, user, cloudService);

		checkIsDeploymentModule(run);

		checkAllImagesHaveReferenceOrImageId(run);
	}

	private static void checkIsDeploymentModule(Run run) throws ValidationException {
		if (!(run.getModule() instanceof DeploymentModule)) {
			throw new ValidationException("Only deployment modules can be deployed");
		}
	}

	private static void checkAllImagesHaveReferenceOrImageId(Run run)
			throws ValidationException {

		DeploymentModule deployment = (DeploymentModule) run.getModule();

		for (Node node : deployment.getNodes().values()) {
			String cloudServiceName = run.getEffectiveCloudServiceName(node);
			ImageModule image = node.getImage();
			if (image == null) {
				throw new ValidationException("Unknown image: " + node.getImageUri());
			}

			try {
				checkImageHasReferenceOrImageId(image, cloudServiceName);
			} catch (ValidationException ex) {
				throw new ValidationException("Node " + node.getName()
						+ " refers to image " + ex.getMessage());
			}
		}
	}

	private static void checkImageHasReferenceOrImageId(ImageModule image,
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
		} else if (image.getModuleReference() == null || "".equals(image.getModuleReference())) {
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

	private static void initNodesInstancesRuntimeParameters(Run run) throws ValidationException,
			NotFoundException {

		List<String> filter = new ArrayList<String>();
		for (ParameterCategory c : ParameterCategory.values()) {
			filter.add(c.toString());
		}

		DeploymentModule deployment = (DeploymentModule) run.getModule();

		for (Node node : deployment.getNodes().values()) {
			for (int i = 1; i <= node.getMultiplicity(); i++) {
				initNodeInstanceRuntimeParameters(run, node, i);
			}
			run.addGroup(node.getName(), run.getEffectiveCloudServiceName(node));
		}

		// mapping
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

	}

	public static Run initNodeInstanceRuntimeParameters(Run run, Node node, int nodeInstanceId)
			throws ValidationException, NotFoundException {

		String cloudServiceName = run.getEffectiveCloudServiceName(node);

		initNodeInstanceCommonRuntimeParameters(run, node, nodeInstanceId);

		run.createRuntimeParameter(node, nodeInstanceId,
				RuntimeParameter.NODE_NAME_KEY, node.getName(),
				RuntimeParameter.NODE_NAME_DESCRIPTION);

		run.createRuntimeParameter(node, nodeInstanceId,
				RuntimeParameter.NODE_ID_KEY, String.valueOf(nodeInstanceId),
				RuntimeParameter.NODE_ID_DESCRIPTION);

		run.createRuntimeParameter(node, nodeInstanceId,
				RuntimeParameter.CLOUD_SERVICE_NAME, cloudServiceName,
				RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);

		ImageModule image = node.getImage();
		String imageId = image.extractBaseImageId(cloudServiceName);
		run.createRuntimeParameter(node, nodeInstanceId,
				RuntimeParameter.IMAGE_ID_PARAMETER_NAME, imageId,
				RuntimeParameter.IMAGE_ID_PARAMETER_DESCRIPTION);

		run = initNodeInstanceRuntimeParametersFromImageParameters(run, node, nodeInstanceId);

		run.addNodeInstanceName(node, nodeInstanceId);

		return run;
	}

	public static void initNodeInstanceCommonRuntimeParameters(Run run, Node node, int nodeInstanceId)
			throws ValidationException {
		assignCommonNodeRuntimeParameters(run, Run.composeNodeInstanceName(node, nodeInstanceId));
	}


	private static Run initNodeInstanceRuntimeParametersFromImageParameters(Run run, Node node, int nodeInstanceId)
			throws ValidationException {

		List<String> filter = new ArrayList<String>();
		for (ParameterCategory c : ParameterCategory.values()) {
			filter.add(c.toString());
		}

		String cloudService = run.getEffectiveCloudServiceName(node);
		ImageModule image = node.getImage();

		for (ModuleParameter param : image.getParameterList()) {
			String category = param.getCategory();
			if (filter.contains(category) || cloudService.equals(category))	{
				String initialValue = extractInitialValue(param, node, run);
				run.createRuntimeParameter(node, nodeInstanceId,
						param.getName(),
						initialValue,
						param.getDescription(),
						param.getType());
			}
		}

		return run;
	}

	private static void initNodesRuntimeParameters(Run run) throws ValidationException {

		DeploymentModule deployment = (DeploymentModule) run.getModule();
		for (Node node : deployment.getNodes().values()) {

			String nodeName = node.getName();
			int multiplicity = node.getMultiplicity();
			int multStartIndex = RuntimeParameter.MULTIPLICITY_NODE_START_INDEX;

			String nodeRunParameterKeyName = constructParamName(nodeName,
					RuntimeParameter.IDS_PARAMETER_NAME);
			ArrayList<String> ids = new ArrayList<String>();
			int maxIndex = multStartIndex == 0 ? (multiplicity - 1)
					: multiplicity;
			for (int i = multStartIndex; i <= maxIndex; i++) {
				ids.add(String.valueOf(i));
			}
			run.assignRuntimeParameter(nodeRunParameterKeyName,
					StringUtils.join(ids.toArray(), ","),
					RuntimeParameter.IDS_PARAMETER_DESCRIPTION);

			run.assignRuntimeParameter(
					constructParamName(nodeName,
							RuntimeParameter.MULTIPLICITY_PARAMETER_NAME),
					String.valueOf(multiplicity),
					RuntimeParameter.MULTIPLICITY_PARAMETER_DESCRIPTION	);
		}
	}

	public static void addParameterMapping(Run run, NodeParameter param, int i) {
		String name = insertMultiplicityIndexInParameterName(param.getValue(), 1);
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

	public static String insertMultiplicityIndexInParameterName(String name,
			int index) {
		String[] parts = name.split(RuntimeParameter.NODE_PROPERTY_SEPARATOR);
		return parts[0] + RuntimeParameter.NODE_MULTIPLICITY_INDEX_SEPARATOR
				+ index + RuntimeParameter.NODE_PROPERTY_SEPARATOR + parts[1];
	}

	public static String extractInitialValue(ModuleParameter parameter, Node node, Run run) {
		String parameterName = parameter.getName();

		String value = run.getParameterValue(constructNodeParamName(node, parameterName), null);
		if (value == null) {
			value = extractInitialValue(node.getParameter(parameterName));
			if (value == null) {
				value = parameter.getValue();
			}
		}

		return value;
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

	protected static void initNodeRunParameters(Run run) throws ValidationException {

		DeploymentModule deployment = (DeploymentModule) run.getModule();
		for (Node node : deployment.getNodes().values()) {

			String key = constructNodeParamName(node, RunParameter.NODE_INCREMENT_KEY);
			run.setParameter(new RunParameter(key, String.valueOf(node.getMultiplicity() + 1),
					RunParameter.NODE_INCREMENT_DESCRIPTION));
		}
	}

	public static String constructNodeParamName(Node node, String parameterName) {
		return constructParamName(node.getName(), parameterName);
	}

	public static Map<String, Node> getNodes(Run run) throws ValidationException {
		Module module = run.getModule(false);
		if (module == null) {
			module = loadModule(run);
		}

		if (module.getCategory() != ModuleCategory.Deployment) {
			throw new SlipStreamInternalException(
					"getNodes can only be used with a Deployment module");
		}

		return ((DeploymentModule) module).getNodes();
	}

	public static HashSet<String> getCloudServicesList(Run run) throws ValidationException {

		HashSet<String> cloudServicesList = new HashSet<String>();

		for (Node node : getNodes(run).values()) {
			String cloudServiceName = run.getCloudServiceNameForNode(node.getName());

			cloudServicesList.add(cloudServiceName);
		}

		return cloudServicesList;
	}

	@Override
	protected void initCloudServices(Run run) throws ValidationException {
		run.setCloudServiceNames(StringUtils.join(getCloudServicesList(run), ","));
	}

	@Override
	protected void addUserFormParametersAsRunParameters(Module module, Run run,
			Map<String, List<Parameter<?>>> userChoices) throws ValidationException {

		Map<String, List<Parameter<?>>> parametersPerNode = userChoices;

		DeploymentModule deployment = (DeploymentModule) module;

		for (Map.Entry<String, List<Parameter<?>>> entry : parametersPerNode.entrySet()) {
			String nodeInstanceName = entry.getKey();
			if (!deployment.getNodes().containsKey(nodeInstanceName)) {
				throw new ValidationException("Unknown node: " + nodeInstanceName);
			}

			Node node = deployment.getNodes().get(nodeInstanceName);

			for (Parameter<?> parameter : entry.getValue()) {
				if (parameter.getName().equals(RuntimeParameter.MULTIPLICITY_PARAMETER_NAME)) {
					String key = constructNodeParamName(node, RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);
					RunParameter rp = new RunParameter(key, extractInitialValue((NodeParameter)parameter),
							RuntimeParameter.MULTIPLICITY_PARAMETER_DESCRIPTION);
					run.setParameter(rp);
					continue;
				}
				if (parameter.getName().equals(RuntimeParameter.CLOUD_SERVICE_NAME)) {
					String key = constructNodeParamName(node, RuntimeParameter.CLOUD_SERVICE_NAME);
					String value = extractInitialValue((NodeParameter)parameter);
					RunParameter rp = new RunParameter(key, run.getEffectiveCloudServiceName(value),
							RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);
					run.setParameter(rp);
					continue;
				}
				if (!node.getParameters().containsKey(parameter.getName())) {
					throw new ValidationException("Unknown parameter: " + parameter.getName() + " in node: "
							+ nodeInstanceName);
				}
				String key = constructNodeParamName(node, parameter.getName());
				RunParameter rp = new RunParameter(key, extractInitialValue((NodeParameter)parameter), "");
				run.setParameter(rp);
			}

		}

	}

}
