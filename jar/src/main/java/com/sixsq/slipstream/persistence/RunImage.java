//package com.sixsq.slipstream.persistence;

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
//
//import org.restlet.data.Status;
//import org.restlet.resource.ResourceException;
//
//import com.sixsq.slipstream.exceptions.NotFoundException;
//import com.sixsq.slipstream.exceptions.SlipStreamException;
//import com.sixsq.slipstream.exceptions.ValidationException;
//
//@SuppressWarnings("serial")
//public class RunImage extends Run {
//
//	public RunImage(String moduleResourceUri, ModuleCategory category)
//			throws SlipStreamException {
//		super(moduleResourceUri, category);
//
//	}
//
//	public void createRunImage(Module module, Run run)
//			throws ValidationException, NotFoundException {
//		// Check that if the ref module doesn't have an image id it
//		// needs a ref image id and recurse until we find an image id
//		if (module.getModuleReference() == null) {
//			throw new ResourceException(
//					Status.CLIENT_ERROR_CONFLICT,
//					"This image doesn't contain a module reference, which means it cannot be built.");
//		}
//
//		// Finding an image id will validate that one exists, which
//		// means we can build this image
//		// FIXME: Fix resolver for latest persistence classes & restlet
//		// Resolver.findReferencedModule(module);
//
//		// Check if the image declares a list of packages or a recipe.
//		// If it doesn't, we don't need to build the image. However, we
//		// need to make sure that the referenced image is built.
//		ImageModule image = (ImageModule) module;
//		if (image.getPackages() == null && image.getRecipe() == null
//				&& image.getPreRecipe() == null) {
//			throw new ResourceException(
//					Status.CLIENT_ERROR_CONFLICT,
//					"This image doesn't need to be built since it doesn't contain a package list nor a pre-recipe or a recipe.");
//		}
//
//		// Add default values for the params as set in the image
//		// definition
//		// Prepend the machine name prefix for the keys
//		if (image.getParameters() != null) {
//			for (ModuleParameter param : image.getParameters().values()) {
//				if (param.getValue() != null) {
//					run.assignRuntimeParameter(
//							Run.MACHINE_NAME_PREFIX + param.getName(),
//							param.getValue(), param.getDescription());
//				}
//			}
//		}
//
//		// Set the initial status of the machine image
//		run.assignRuntimeParameter(Run.MACHINE_NAME_PREFIX
//				+ RuntimeParameter.STATE_MESSAGE_KEY,
//				Run.INITIAL_NODE_STATE_MESSAGE,
//				RuntimeParameter.STATE_MESSAGE_DESCRIPTION);
//		run.assignRuntimeParameter(Run.MACHINE_NAME_PREFIX
//				+ RuntimeParameter.STATE_KEY, Run.INITIAL_NODE_STATE,
//				RuntimeParameter.STATE_DESCRIPTION);
//	}
//
//}
