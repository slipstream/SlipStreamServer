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
import java.util.List;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidMetadataException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ParameterType;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;

public class BuildImageFactory extends RunFactory {

	@Override
	protected Run constructRun(Module module, String cloudService, User user)
			throws ValidationException {
		return new Run(module, RunType.Machine, cloudService, user);
	}

	@Override
	protected void validateModule(Module module, String cloudService)
			throws SlipStreamClientException {

		ImageModule image = (ImageModule) module;
		if (image.isBase()) {
			throw new SlipStreamClientException("A base image cannot be built");
		}

		checkNoCircularDependencies(image);

		checkHasSomethingToBuild(image);

		checkNotAlreadyBuilt(image, cloudService);

		// Finding an image id will validate that one exists
		image.extractBaseImageId(cloudService);
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
	protected void initialize(Module module, Run run, User user, String cloudService)
			throws ValidationException, NotFoundException {

		super.initialize(module, run, user, cloudService);

		initializeOrchestrtorRuntimeParameters(run);
		initRuntimeParameters((ImageModule) module, run);
		initMachineState(run);
		initNodeNames(run, cloudService);
		initOrchestratorsNodeNames(run);
	}

	protected static void initMachineState(Run run) throws ValidationException,
			NotFoundException {

		assignRuntimeParameters(run, Run.MACHINE_NAME);
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
		String cloudService = run.getCloudService();
		filter.add(cloudService);

		if (image.getParameters() != null) {
			for (ModuleParameter param : image.getParameterList()) {
				if (filter.contains(param.getCategory())) {
					run.assignRuntimeParameter(
							Run.MACHINE_NAME_PREFIX + param.getName(),
							param.getValue(), param.getDescription());
				}
			}
		}

		// Add cloud service name to orchestrator and machine
		String cloudServiceName = run.getCloudService();
		run.assignRuntimeParameter(Run.MACHINE_NAME_PREFIX
				+ RuntimeParameter.CLOUD_SERVICE_NAME, cloudServiceName,
				RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);

		String imageId = image.extractBaseImageId(cloudService);
		run.assignRuntimeParameter(Run.MACHINE_NAME_PREFIX + RuntimeParameter.IMAGE_ID_PARAMETER_NAME, imageId,
				RuntimeParameter.IMAGE_ID_PARAMETER_DESCRIPTION, ParameterType.String);

	}

	protected void initNodeNames(Run run, String cloudService)
			throws ConfigurationException, ValidationException {
		run.addNodeName(Run.MACHINE_NAME, cloudService);
		run.addGroup(Run.MACHINE_NAME, cloudService);
	}

	@Override
	public Module overloadModule(Run run, User user) throws ValidationException {
		Module module = loadModule(run);
		return ImageModule.populateBaseImageIdFromRun(run, module);
	}

}
