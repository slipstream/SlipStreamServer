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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.sixsq.slipstream.connector.CloudService;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidMetadataException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ParameterType;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunParameter;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;


public class BuildImageFactory extends RunFactory {

	protected final static String nodeInstanceName = Run.MACHINE_NAME;

	@Override
	protected RunType getRunType() {
		return RunType.Machine;
	}

	@Override
	protected void validateModule(Module module, Map<String, String> cloudServicePerNode)
			throws SlipStreamClientException {

		ImageModule image = castToRequiredModuleType(module);
		if (image.isBase()) {
			throw new SlipStreamClientException("A base image cannot be built");
		}

		checkNoCircularDependencies(image);

		checkHasSomethingToBuild(image);

		String cloudServiceName = cloudServicePerNode.get(nodeInstanceName);
		checkNotAlreadyBuilt(image, cloudServiceName);

		// Finding an image id will validate that one exists
		image.extractBaseImageId(cloudServiceName);
	}

	protected static void checkNoCircularDependencies(ImageModule image)
			throws InvalidMetadataException, ValidationException {
		List<String> visitedReferenceModules = new ArrayList<String>();
		recurseDependencies(image, visitedReferenceModules);
	}

	private static void recurseDependencies(ImageModule image,
			List<String> visitedReferenceModules)
			throws InvalidMetadataException, ValidationException {
		for (String ref : visitedReferenceModules) {
			if (ref.equals(image.getResourceUri())) {
				throw new InvalidMetadataException(
						"Circular dependency detected in module "
								+ image.getResourceUri());
			}
		}
		visitedReferenceModules.add(image.getResourceUri());
		if (image.getModuleReference() != null) {
			recurseDependencies(
					(ImageModule) ImageModule.load(image.getModuleReference()),
					visitedReferenceModules);
		}
		return;
	}

	private static void checkHasSomethingToBuild(ImageModule image)
			throws ValidationException {
		// Check if the image declares a list of packages or a recipe.
		// If it doesn't, we don't need to build the image. However, we
		// need to make sure that the referenced image is built.
		if (image.isVirtual()) {
			throw new ValidationException(
					"This image doesn't need to be built since it doesn't contain a package list nor a pre-recipe or a recipe.");
		}
	}

	private static void checkNotAlreadyBuilt(ImageModule image,
			String cloudServiceName) throws ValidationException {
		// Check that the image is not already built for this cloud service name
		if (image.getCloudImageIdentifier(cloudServiceName) != null) {
			throw new ValidationException(
					"This image was already built for cloud: "
							+ cloudServiceName);
		}
	}

	@Override
	protected void init(Module module, Run run, User user)
			throws ValidationException, NotFoundException {

		initRuntimeParameters((ImageModule) module, run);
		initMachineState(run);

		String cloudService = run.getCloudServiceNameForNode(nodeInstanceName);
		initNodeNames(run, cloudService);
	}

	protected static void initMachineState(Run run) throws ValidationException,
			NotFoundException {

		assignCommonNodeInstanceRuntimeParameters(run, nodeInstanceName);
	}

	protected static void initRuntimeParameters(ImageModule image, Run run)
			throws ValidationException, NotFoundException {

		// Add default values for the params as set in the image
		// definition
		// Only process the standard categories and the cloud service
		// (not the other cloud services, if any)

		List<String> filter = new ArrayList<String>();
		for (ParameterCategory c : ParameterCategory.values()) {
			filter.add(c.toString());
		}
		String cloudServiceName = run.getCloudServiceNameForNode(nodeInstanceName);
		filter.add(cloudServiceName);

		Map<String, ModuleParameter> parameters = getConnectorParameters(cloudServiceName);

		List<String> cloudParameters = getConnectorParametersAsKeysList(parameters, cloudServiceName);

		for(RunParameter p : run.getParameterList()) {
			String paramWithoutPrefix = removePrefixParameter(p.getName(), nodeInstanceName);
			if (cloudParameters.contains(paramWithoutPrefix)) {
				RunParameter rp = run.getParameters().get(p.getName());
				rp.setCategory(cloudServiceName);
				rp.setName(constructParamName(nodeInstanceName,
						Parameter.constructKey(cloudServiceName, paramWithoutPrefix)));
				run.setParameter(rp);
			}
		}

		parameters.putAll(image.getParameters());

		ImageModule parent = image.getParentModule();
		if (parent != null) {
			findAndAddImagesApplicationParameters(parameters, parent);
		}

		for (ModuleParameter param : parameters.values()) {
			if (filter.contains(param.getCategory())) {
				run.assignRuntimeParameter(constructParamName(nodeInstanceName, param.getName()),
						extractInitialValue(param, image, run, cloudServiceName),
						param.getDescription());
			}
		}

		// Add cloud service name to orchestrator and machine
		run.assignRuntimeParameter(constructParamName(nodeInstanceName, RuntimeParameter.CLOUD_SERVICE_NAME),
				cloudServiceName, RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);

		String imageId = image.extractBaseImageId(cloudServiceName);
		run.assignRuntimeParameter(constructParamName(nodeInstanceName, RuntimeParameter.IMAGE_ID_PARAMETER_NAME),
				imageId, RuntimeParameter.IMAGE_ID_PARAMETER_DESCRIPTION, ParameterType.String);

		String imagePlatform = image.getPlatform();
		run.assignRuntimeParameter(constructParamName(nodeInstanceName, RuntimeParameter.IMAGE_PLATFORM_PARAMETER_NAME),
				imagePlatform, RuntimeParameter.IMAGE_PLATFORM_PARAMETER_DESCRIPTION, ParameterType.String);

		String serviceOfferId = run.getParameterValue(
				constructParamName(nodeInstanceName, RuntimeParameter.SERVICE_OFFER), "");
		run.assignRuntimeParameter(constructParamName(nodeInstanceName, RuntimeParameter.SERVICE_OFFER),
				serviceOfferId, RuntimeParameter.SERVICE_OFFER_DESCRIPTION, ParameterType.String);

		if (serviceOfferId != null && !serviceOfferId.isEmpty()) {
			applyServiceOffer(run, nodeInstanceName, cloudServiceName, serviceOfferId);
		}
	}

	private static String extractInitialValue(ModuleParameter parameter, ImageModule image, Run run,
											  String cloudService) throws ValidationException {
		String parameterName = parameter.getName();

		String value = run.getParameterValue(constructParamName(nodeInstanceName, parameterName), null);
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

	protected void initNodeNames(Run run, String cloudService)
			throws ConfigurationException, ValidationException {
		run.addNodeInstanceName(nodeInstanceName, cloudService);
		run.addGroup(nodeInstanceName, cloudService);
	}

	@Override
	protected void addUserFormParametersAsRunParameters(Module module, Run run,
			Map<String, List<Parameter<?>>> userChoices) throws ValidationException {

		if (!isProvidedUserChoicesForNodeInstance(userChoices, nodeInstanceName)) {
				return;
		}

		ImageModule image = (ImageModule) module;
		List<Parameter<?>> userChoicesForMachine = userChoices.get(nodeInstanceName);

		for (Parameter<?> parameter : userChoicesForMachine) {
			checkParameterIsValid(image, parameter, run.getCloudServiceNameForNode(nodeInstanceName));

			String key = constructParamName(nodeInstanceName, parameter.getName());
			RunParameter rp = new RunParameter(key, parameter.getValue(), "");
			run.setParameter(rp);
		}
	}

	// TODO: pull this method up to be used in other factories.
	public static boolean isProvidedUserChoicesForNodeInstance(Map<String, List<Parameter<?>>> userChoices,
	        String nodeInstanceName) {
		if (userChoices == null || userChoices.isEmpty())
			return false;

		List<Parameter<?>> paramsForNodeInstance = userChoices.get(nodeInstanceName);
		if (paramsForNodeInstance == null || paramsForNodeInstance.isEmpty())
			return false;

		return true;
	}

	private static void checkParameterIsValid(ImageModule image, Parameter<?> parameter, String cloudServiceName)
			throws ValidationException {
		List<String> paramsToFilter = new ArrayList<String>();
		paramsToFilter.add(RuntimeParameter.MULTIPLICITY_PARAMETER_NAME);
		paramsToFilter.add(RuntimeParameter.CLOUD_SERVICE_NAME);
		paramsToFilter.add(RuntimeParameter.SERVICE_OFFER);
		paramsToFilter.addAll(
				getConnectorParametersAsKeysList(getConnectorParameters(cloudServiceName), cloudServiceName));

		String paramName = parameter.getName();
		if (!image.getParameters().containsKey(paramName)
				&& !image.getInputParametersExpanded().containsKey(paramName)
				&& !paramsToFilter.contains(paramName))
		{
			throw new ValidationException("Unknown parameter: " + parameter.getName() + " in node: " +nodeInstanceName);
		}
	}

	protected static void checkServiceOfferCloudService(String connectorHref, String cloudService) throws ValidationException {
		if (cloudService != null && !cloudService.equals(connectorHref)) {
			throw new ValidationException("The service offer is for '" + connectorHref +
					"' but the Cloud service '" + cloudService + "' was asked. " +
					"You don't need to specify a Cloud service if you specify a service offer.");
		}
	}

	@Override
	protected Map<String, String> resolveCloudServiceNames(Module module, User user,
			Map<String, List<Parameter<?>>> userChoices) throws ValidationException {
		Map<String, String> cloudServiceNamesPerNode = new HashMap<String, String>();
		String cloudService = null;

		if (isProvidedUserChoicesForNodeInstance(userChoices, nodeInstanceName)) {
    		for (Parameter<?> parameter : userChoices.get(nodeInstanceName)) {
    			if (parameter.getName().equals(RuntimeParameter.SERVICE_OFFER)) {
    				String value = parameter.getValue();
					if (value != null && !value.isEmpty()) {
						String cs = getCloudServiceFromServiceOffer(value);
						if (cloudService != null) {
							checkServiceOfferCloudService(cs, cloudService);
						}
						cloudService = cs;
					}
    			} else if (parameter.getName().equals(RuntimeParameter.CLOUD_SERVICE_NAME)) {
					String cs = parameter.getValue();
					if (cloudService != null) {
						checkServiceOfferCloudService(cloudService, cs);
					} else {
						cloudService = cs;
					}
				}
    		}
		}

		if (CloudService.isDefaultCloudService(cloudService)) {
			cloudService = user.getDefaultCloudService();
		}

		cloudServiceNamesPerNode.put(nodeInstanceName, cloudService);

		return cloudServiceNamesPerNode;
	}

	@Override
	protected void initExtraRunParameters(Module module, Run run) throws ValidationException {
	}

	@Override
	protected void updateExtraRunParameters(Module module, Run run, Map<String, List<Parameter<?>>> userChoices)
			throws ValidationException {
	}

	@Override
    protected ImageModule castToRequiredModuleType(Module module) {
	    return (ImageModule) module;
    }

}

