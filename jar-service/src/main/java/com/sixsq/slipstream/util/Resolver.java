package com.sixsq.slipstream.util;

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

import com.sixsq.slipstream.exceptions.InvalidMetadataException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Parameter;

public class Resolver {

	// FIXME: Check for circular references.
	public static ImageModule findReferencedModule(ImageModule module,
			String cloudService) throws InvalidMetadataException,
			ValidationException {

		String moduleReference = module.getModuleReferenceUri();

		if (moduleReference == null) {
			throw new ValidationException("Module " + module.getName()
					+ " is missing a references module");
		}

		ImageModule reference = (ImageModule) ImageModule.load(moduleReference);

		if (reference == null) {
			throw new InvalidMetadataException("Module " + module.getName()
					+ " references module " + moduleReference
					+ " which doesn't exist");
		}

		// If the image id is set, the image exists and we stop the processing
		if (Parameter.hasValueSet(reference.getCloudImageId(cloudService))) {
			return reference;
		}

		if (reference.isBase()) {
			throw new InvalidMetadataException(
					"Missing image id for base image: " + module.getName()
							+ " on cloud service: " + cloudService);
		}

		if (reference.getModuleReferenceUri() != null) {
			return Resolver.findReferencedModule(reference, cloudService);
		}

		throw new InvalidMetadataException("Module '" + module.getName()
				+ "' references module '" + moduleReference
				+ "' which is not base and doesn't define a module reference");
	}

	/**
	 * Resolve references by processing the hierarchy (linear for the moment) of
	 * module references. If the referenced module has an image id defined, stop
	 * the processing since we know we have an image to start the process from.
	 * 
	 * @param baseModule
	 * @return the input versionBase with its image id filled from referenced
	 *         information if successful.
	 * @throws InvalidMetadataException
	 * @throws ValidationException
	 */
	public static ImageModule resolveReference(ImageModule baseModule,
			String cloudServiceName) throws InvalidMetadataException,
			ValidationException {

		ImageModule referencedModule = Resolver.findReferencedModule(
				baseModule, cloudServiceName);

		if (referencedModule.getCloudImageId(cloudServiceName) != null) {

			return baseModule;

		} else {

			if (referencedModule.getModuleReferenceUri() != null) {

				return Resolver.resolveReference(referencedModule,
						cloudServiceName);

			} else {
				throw new InvalidMetadataException(
						"Referenced module '"
								+ referencedModule.getName()
								+ "' doesn't contain neither an image id nor a module reference. "
								+ "We therefore cannot process the module dependency chain.");
			}
		}
	}

}
