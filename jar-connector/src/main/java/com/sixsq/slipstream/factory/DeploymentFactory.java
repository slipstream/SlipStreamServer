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

import com.sixsq.slipstream.connector.CloudService;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
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
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentFactory extends RunFactory {

	@Override
	protected RunType getRunType() {
		return RunType.Orchestration;
	}

	@Override
	protected void init(Module module, Run run, User user) throws ValidationException, NotFoundException {
		initNodesInstancesRuntimeParameters(run);
		initNodesRuntimeParameters(run);
	}

	@Override
	protected void validateRun(Run run, User user)
			throws SlipStreamClientException {

		super.validateRun(run, user);

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
			String cloudServiceName = run.getCloudServiceNameForNode(node.getName());
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

	private static void checkImageHasReferenceOrImageId(ImageModule image, String cloudServiceName)
			throws ValidationException {

		if (!"".equals(image.getCloudImageId(cloudServiceName))) {
			return;
		}

		boolean mustHaveImageId = image.isBase();
		if (mustHaveImageId && "".equals(image.getCloudImageId(cloudServiceName))) {
			throw new ValidationException(image.getName() + " missing an image id for cloud: " + cloudServiceName);

		} else if (image.getModuleReference() == null || "".equals(image.getModuleReference())) {
			throw new ValidationException(image.getName() + " missing a machine image reference");

		} else {
			String referenceUri = image.getModuleReference();
			ImageModule reference = (ImageModule) ImageModule.load(referenceUri);
			if (reference == null) {
				throw new ValidationException(image.getName() + " referring to an unknown image "
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
			for (int i = 1; i <= getNodeMultiplicity(run, node); i++) {
				initNodeInstanceRuntimeParameters(run, node, i);
			}
			run.addGroup(node.getName(), run.getCloudServiceNameForNode(node.getName()));
		}

		// mapping
		for (Node node : deployment.getNodes().values()) {
			int multiplicity = getNodeMultiplicity(run, node);
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

		String cloudServiceName = run.getCloudServiceNameForNode(node.getName());

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

		String imagePlatform = image.getPlatform();
		run.createRuntimeParameter(node, nodeInstanceId,
				RuntimeParameter.IMAGE_PLATFORM_PARAMETER_NAME, imagePlatform,
				RuntimeParameter.IMAGE_PLATFORM_PARAMETER_DESCRIPTION);

		run = initNodeInstanceRuntimeParametersFromImageParameters(run, node, nodeInstanceId);

		run.addNodeInstanceName(node, nodeInstanceId);

		return run;
	}

	public static void initNodeInstanceCommonRuntimeParameters(Run run, Node node, int nodeInstanceId)
			throws ValidationException {
		assignCommonNodeInstanceRuntimeParameters(run, Run.composeNodeInstanceName(node, nodeInstanceId));
	}


	private static Run initNodeInstanceRuntimeParametersFromImageParameters(Run run, Node node, int nodeInstanceId)
			throws ValidationException {

		List<String> filter = new ArrayList<String>();
		for (ParameterCategory c : ParameterCategory.values()) {
			filter.add(c.toString());
		}

		String cloudService = run.getCloudServiceNameForNode(node.getName());
		ImageModule image = node.getImage();
		Map<String, ModuleParameter> parameters = getConnectorParameters(cloudService);

		List<String> cloudParameters = getConnectorParametersAsKeysList(parameters, cloudService);

		for(RunParameter p : run.getParameterList()) {
			String paramWithoutPrefix = removePrefixParameter(p.getName(), node.getName());
			if (cloudParameters.contains(paramWithoutPrefix)) {
				RunParameter rp = run.getParameters().get(p.getName());
				rp.setCategory(cloudService);
				rp.setName(constructParamName(node.getName(),
						Parameter.constructKey(cloudService, paramWithoutPrefix)));
				run.setParameter(rp);
			}
		}

		parameters.putAll(image.getParameters());

		ImageModule parent = image.getParentModule();
		if (parent != null) {
			findAndAddImagesApplicationParameters(parameters, parent);
		}

		for (ModuleParameter param : parameters.values()) {
			String category = param.getCategory();
			if (filter.contains(category) || cloudService.equals(category))	{
				String initialValue = extractInitialValue(param, node, run, cloudService);
				run.createRuntimeParameter(node, nodeInstanceId,
						param.getName(),
						initialValue,
						param.getDescription(),
						param.getType());
			}
		}

		return run;
	}

	private static void initNodesRuntimeParameters(Run run) throws ValidationException, NotFoundException {

		DeploymentModule deployment = (DeploymentModule) run.getModule();
		for (Node node : deployment.getNodes().values()) {

			String nodeName = node.getName();
			int multiplicity = getNodeMultiplicity(run, node);
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
					RuntimeParameter.MULTIPLICITY_PARAMETER_DESCRIPTION);

			run.assignRuntimeParameter(
					constructParamName(nodeName,
							RuntimeParameter.MAX_PROVISIONING_FAILURES),
							getMaxProvisioningFailures(run, node),
							RuntimeParameter.MAX_PROVISIONING_FAILURES_DESCRIPTION);
		}
	}

	public static void addParameterMapping(Run run, NodeParameter param, int i) {
		String name = insertMultiplicityIndexInParameterName(param.getValue(), 1);
		RuntimeParameter input = run.getRuntimeParameters().get(name);
		if (null != input) {
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
	}

	public static String insertMultiplicityIndexInParameterName(String name,
			int index) {
		String[] parts = name.split(RuntimeParameter.NODE_PROPERTY_SEPARATOR);
		return parts[0] + RuntimeParameter.NODE_MULTIPLICITY_INDEX_SEPARATOR
				+ index + RuntimeParameter.NODE_PROPERTY_SEPARATOR + parts[1];
	}

	public static String extractInitialValue(ModuleParameter parameter, Node node, Run run, String cloudService)
			throws ValidationException {
		String parameterName = parameter.getName();
		ImageModule image = node.getImage();

		String value = run.getParameterValue(constructNodeParamName(node, parameterName), null);
		if (value == null) {
			value = extractNodeParameterValue(node.getParameter(parameterName));
		}
		if (value == null && image != null && cloudService.equals(parameter.getCategory())) {
			ModuleParameter imageParameter = image.getParameter(parameterName);
			if (imageParameter != null) {
				value = imageParameter.getValue();
			}
		}
		if (value == null) {
			value = parameter.getValue();
		}

		return value;
	}

	private static String extractNodeParameterValue(NodeParameter parameter) {

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

	@Override
	protected void addUserFormParametersAsRunParameters(Module module, Run run,
			Map<String, List<Parameter<?>>> userChoices) throws ValidationException {

		List<String> ignoreList = new ArrayList<String>();
		ignoreList.add(RuntimeParameter.CLOUD_SERVICE_NAME);

		Map<String, List<Parameter<?>>> parametersPerNode = userChoices;

		DeploymentModule deployment = (DeploymentModule) module;

		for (Map.Entry<String, List<Parameter<?>>> entry : parametersPerNode.entrySet()) {
			String nodeName = entry.getKey();
			if (!deployment.getNodes().containsKey(nodeName)) {
				throw new ValidationException("Unknown node: " + nodeName);
			}

			Node node = deployment.getNodes().get(nodeName);

			for (Parameter<?> parameter : entry.getValue()) {
				checkParameterIsValid(node, parameter, run.getCloudServiceNameForNode(nodeName));

				String value = extractNodeParameterValue((NodeParameter)parameter);
				insertNewRunParameterForNode(run, node, parameter.getName(), value, "", ignoreList);
			}
		}
	}



	private void checkParameterIsValid(Node node, Parameter<?> parameter, String cloudServiceName) throws ValidationException {
		List<String> paramsToFilter = new ArrayList<String>();
		paramsToFilter.add(RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);
		paramsToFilter.add(RuntimeParameter.CLOUD_SERVICE_NAME);
		paramsToFilter.add(RuntimeParameter.MAX_PROVISIONING_FAILURES);

		paramsToFilter.addAll(getConnectorParametersAsKeysList(
				getConnectorParameters(cloudServiceName), cloudServiceName));


		String paramName = parameter.getName();
		if (!node.getParameters().containsKey(paramName) &&
				!node.getImage().getParameters().containsKey(paramName) &&
				!paramsToFilter.contains(paramName)) {
			throw new ValidationException("Unknown parameter: " + parameter.getName() + " in node: "
					+ node.getName());
		}
	}

	@Override
	protected Map<String, String> resolveCloudServiceNames(Module module, User user,
			Map<String, List<Parameter<?>>> userChoices) {
		Map<String, String> cloudServiceNamesPerNode = new HashMap<String, String>();
		DeploymentModule deployment = castToRequiredModuleType(module);

		for (Node node: deployment.getNodes().values()) {
			String nodeName = node.getName();
			List<Parameter<?>> userChoicesForNode = userChoices.get(nodeName);
			String cloudServiceName = resolveCloudServiceNameForNode(user, userChoicesForNode, node);
			cloudServiceNamesPerNode.put(nodeName, cloudServiceName);
		}

		return cloudServiceNamesPerNode;
	}

	@Override
	protected DeploymentModule castToRequiredModuleType(Module module) {
		return (DeploymentModule) module;
	}

	private String resolveCloudServiceNameForNode(User user, List<Parameter<?>> userChoicesForNode, Node node) {
		String cloudService = null;

		if (userChoicesForNode != null) {
			for (Parameter<?> parameter : userChoicesForNode) {
				if (parameter.getName().equals(RuntimeParameter.CLOUD_SERVICE_NAME)) {
					cloudService = extractNodeParameterValue((NodeParameter) parameter);
					break;
				}
			}
		}

		if (cloudService == null) {
			cloudService = node.getCloudService();
		}

		if (CloudService.isDefaultCloudService(cloudService)) {
			cloudService = user.getDefaultCloudService();
		}

		return cloudService;
	}

	@Override
	protected void initExtraRunParameters(Module module, Run run) throws ValidationException {

		DeploymentModule deployment = (DeploymentModule) run.getModule();
		for (Node node : deployment.getNodes().values()) {
			int multiplicity = node.getMultiplicity();

			insertNewRunParameterForNode(run, node,
					RuntimeParameter.MULTIPLICITY_PARAMETER_NAME, String.valueOf(multiplicity),
					RuntimeParameter.MULTIPLICITY_PARAMETER_DESCRIPTION);

			insertNewRunParameterForNode(run, node,
					RuntimeParameter.MAX_PROVISIONING_FAILURES, String.valueOf(node.getMaxProvisioningFailures()),
					RuntimeParameter.MAX_PROVISIONING_FAILURES_DESCRIPTION);

			insertNewRunParameterForNode(run, node,
					RunParameter.NODE_INCREMENT_KEY, String.valueOf(multiplicity + 1),
					RunParameter.NODE_INCREMENT_DESCRIPTION);

			String cloudService = run.getParameterValue(constructParamName(
					node.getName(),
					RuntimeParameter.CLOUD_SERVICE_NAME), CloudImageIdentifier.DEFAULT_CLOUD_SERVICE);
			boolean runBuildRecipes = node.getImage().hasToRunBuildRecipes(cloudService);
			insertNewRunParameterForNode(run, node,
					RunParameter.NODE_RUN_BUILD_RECIPES_KEY, String.valueOf(runBuildRecipes),
					RunParameter.NODE_RUN_BUILD_RECIPES_DESCRIPTION);
		}
	}

	@Override
	protected void updateExtraRunParameters(Module module, Run run, Map<String, List<Parameter<?>>> userChoices)
			throws ValidationException {
		DeploymentModule deployment = (DeploymentModule) run.getModule();
		for (Node node : deployment.getNodes().values()) {

			List<Parameter<?>> params = userChoices.get(node.getName());
			if (params != null) {
				for (Parameter<?> parameter : params) {
					if (parameter.getName().equals(RuntimeParameter.MULTIPLICITY_PARAMETER_NAME)){
						String key = constructNodeParamName(node, RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);
						String multiplicity = extractNodeParameterValue((NodeParameter)parameter);
						run.getParameter(key).setValue(multiplicity);

						key = constructNodeParamName(node, RunParameter.NODE_INCREMENT_KEY);
						String increment = String.valueOf(Integer.parseInt(multiplicity) + 1);
						run.getParameter(key).setValue(increment);
					} else if (parameter.getName().equals(RuntimeParameter.MAX_PROVISIONING_FAILURES)) {
						String key = constructNodeParamName(node, RuntimeParameter.MAX_PROVISIONING_FAILURES);
						String value = extractNodeParameterValue((NodeParameter)parameter);
						run.getParameter(key).setValue(value);
					}
				}
			}
		}
	}

	protected void validateModuleForRun(Module module) throws ValidationException {
		super.validateModuleForRun(module);

		// Deployment module that is ready for run should define nodes on itself.
		DeploymentModule deploymentModule = castToRequiredModuleType(module);
		if (deploymentModule.getNodes().isEmpty()) {
			throw new ValidationException("Deployment module provided for run is missing nodes.");
		}
	}

	private static int getNodeMultiplicity(Run run, Node node) throws NotFoundException {
		String key = constructNodeParamName(node, RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);
		return Integer.parseInt(run.getParameterValue(key, null));
	}

	private static String getMaxProvisioningFailures(Run run, Node node) throws NotFoundException {
		String key = constructNodeParamName(node, RuntimeParameter.MAX_PROVISIONING_FAILURES);
		return run.getParameterValue(key, null);
	}

	private static void insertNewRunParameterForNode(Run run, Node node, String name, String value, String description)
			throws ValidationException {
		insertNewRunParameterForNode(run, node, name, value, description, new ArrayList<String>());
	}

	private static void insertNewRunParameterForNode(Run run, Node node, String name, String value, String description,
			List<String> ignore) throws ValidationException {
		if (ignore.contains(name)) {
			return;
		}
		String key = constructNodeParamName(node, name);
		RunParameter rp = new RunParameter(key, value, description);
		run.setParameter(rp);
	}

}


