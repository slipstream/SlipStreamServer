package com.sixsq.slipstream.module;

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.restlet.data.Form;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.ParametersFactory;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Package;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.Target;
import com.sixsq.slipstream.persistence.User;

public class ImageFormProcessor extends ModuleFormProcessor {

	public static final String PRERECIPE_SCRIPT_NAME = "prerecipe--script";
	// private static final String TARGET_PRERECIPE_NAME = "prerecipe";
	// private static final String TARGET_RECIPE_NAME = "recipe";
	// private static final String[] TARGET_BUILD_SCRIPT_NAMES = {
	// TARGET_PRERECIPE_NAME, TARGET_RECIPE_NAME };
	// private static final String[] TARGET_SCRIPT_NAMES = {
	// TARGET_PRERECIPE_NAME, TARGET_RECIPE_NAME, "execute", "report",
	// "onvmadd", "onvmremove" };
	public static final String MODULE_REFERENCE_NAME = "moduleReference";

	boolean needsRebuild = false;

	public ImageFormProcessor(User user) {
		super(user);
	}

	@Override
	protected Module getOrCreateParameterized(String name) throws ValidationException, NotFoundException {
		Module loaded = load(name);
		return loaded == null ? new ImageModule(name) : loaded;
	}

	@Override
	public void parseForm() throws ValidationException, NotFoundException {

		super.parseForm();

		ImageModule module = castToModule();
		ParametersFactory.addParametersForEditing(module);

		parseReferenceImageAndIsBase(getForm());
		parsePackages(getForm());
		parseNewImage(getForm());
		parseTargets(getForm());

		parseImageId(getForm());
	}

	private void parsePackages(Form form) throws ValidationException {

		// items should be encoded as: package--[index]--[value]
		Set<String> formitems = form.getNames();

		ImageModule image = castToModule();
		image.getPackages().clear();

		for (String inputName : formitems.toArray(new String[0])) {
			if (inputName.startsWith("package--") && inputName.endsWith("--name")) {
				String[] parts = inputName.split("--");
				String genericPart = parts[0] + "--" + parts[1] + "--";
				String name = form.getFirstValue(inputName);
				String repository = form.getFirstValue(genericPart + "repository");
				String key = form.getFirstValue(genericPart + "key");
				Package package_ = new Package(name, repository, key);
				if (image.getPackages().contains(package_)) {
					throw (new ValidationException("Cannot specify the same package multiply times: "
							+ package_.getName()));
				}
				image.setPackage(package_);
			}
		}

		if (havePackagesChanged(image.getPackages())) {
			needsRebuild = true;
		}

	}

	protected boolean havePackagesChanged(Set<Package> packages) {
		boolean haveChanged = false;
		Set<Package> oldPackages = castToModule().getPackages();
		if (packages.size() != oldPackages.size()) {
			haveChanged = true;
		} else {
			for (Package p : packages) {
				if (!oldPackages.contains(p)) {
					haveChanged = true;
					break;
				}
			}
		}
		return haveChanged;
	}

	private void parseReferenceImageAndIsBase(Form form) throws ValidationException {
		ImageModule module = castToModule();
		String moduleReference = form.getFirstValue(MODULE_REFERENCE_NAME, "");
		String newReferenceImage = Module.constructResourceUri(moduleReference);
		String oldReferenceImage = module.getModuleReferenceUri();
		Boolean newIsBase = getBooleanValue(form, "isbase");
		Boolean oldIsBase = module.isBase();

		if (!newIsBase && Parameter.hasValueSet(moduleReference)) {
			module.setModuleReference(newReferenceImage);
		}

		module.setIsBase(newIsBase);

		if (oldReferenceImage == null || oldReferenceImage.trim().isEmpty()) {
			oldReferenceImage = Module.constructResourceUri("");
		}

		if (newIsBase != oldIsBase || !newReferenceImage.equals(oldReferenceImage)) {
			needsRebuild = true;
		}
	}

	private void parseTargets(Form form) throws ValidationException {

		Map<String, Target> targets = new HashMap<String, Target>();

		for (String targetName : Target.TARGET_SCRIPT_NAMES) {
			addTarget(form, targets, targetName);
		}

		castToModule().setTargets(targets);

	}

	private void addTarget(Form form, Map<String, Target> targets, String targetName) {
		String script = form.getFirstValue(targetName + "--script");
		Target target = new Target(targetName, script);
		if (script != null) {
			targets.put(targetName, target);
		}
		if (Arrays.asList(Target.TARGET_BUILD_SCRIPT_NAMES).contains(targetName)) {
			if (hasBuildTargetChanged(target)) {
				needsRebuild = true;
			}
		}
	}

	private boolean hasBuildTargetChanged(Target newTarget) {

		boolean newTargetScriptSet  = Parameter.hasValueSet(newTarget.getScript());
		Map<String, Target> oldTargets = castToModule().getTargets();

		Target oldTarget = oldTargets.get(newTarget.getName());
		if(oldTarget == null) {
			return newTargetScriptSet;
		}

		return !oldTarget.equals(newTarget);
	}

	private void parseImageId(Form form) throws ValidationException {
		ImageModule module = castToModule();
		// only clean the image id for template images (i.e. not native) that
		// don't need rebuilding
		if (!module.isBase() && needsRebuild) {
			module.setCloudImageIdentifiers(new HashSet<CloudImageIdentifier>());
			return;
		}
		// take updates for base (native) image
		if (module.isBase()) {
			for (String cloudServiceName : ConnectorFactory.getCloudServiceNames()) {
				String imageId = form.getFirstValue(constructFormImageIdName(cloudServiceName));
				module.setImageId(imageId, cloudServiceName);
			}
		}
	}

	public static String constructFormImageIdName(String cloudServiceName) {
		return "cloudimageid_imageid_" + cloudServiceName;
	}

	private void parseNewImage(Form form) {
		parseLoginUser(form);
		parsePlatform(form);
	}

	private void parseLoginUser(Form form) {

		String loginUser = form.getFirstValue("loginUser");
		if (loginUser == null) {
			loginUser = "";
		}

		castToModule().setLoginUser(loginUser);

	}

	private void parsePlatform(Form form) {

		String platform = form.getFirstValue("platform");

		castToModule().setPlatform(platform);

	}

	private ImageModule castToModule() {
		return (ImageModule) getParametrized();
	}

}
