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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.restlet.data.Form;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Package;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.Target;
import com.sixsq.slipstream.persistence.User;

public class ImageFormProcessor extends ModuleFormProcessor {

	boolean needsRebuild = false;

	public ImageFormProcessor(User user) {
		super(user);
	}

	@Override
	protected Module getOrCreateParameterized(String name)
			throws ValidationException, NotFoundException {
		return new ImageModule(name);
	}

	@Override
	public void parseForm() throws ValidationException, NotFoundException {

		super.parseForm();

		ImageModule module = castToModule();

		String moduleReferenceUri = getForm().getFirstValue("moduleReference");
		if (Parameter.hasValueSet(moduleReferenceUri)) {
			module.setModuleReference(Module
					.constructResourceUri(moduleReferenceUri));
		}
		module.setIsBase(getBooleanValue(getForm(), "isbase"));

		parsePackages(getForm());
		parsePreRecipe(getForm());
		parseRecipe(getForm());
		parseNewImage(getForm());
		parseTargets(getForm());

		parseImageId(module);
	}

	private void parsePackages(Form form) throws ValidationException {

		// items should be encoded as: package--[index]--[value]
		Set<String> formitems = form.getNames();
		List<Package> packages = new ArrayList<Package>();

		for (String inputName : formitems.toArray(new String[0])) {
			if (inputName.startsWith("package--")
					&& inputName.endsWith("--name")) {
				String[] parts = inputName.split("--");
				String genericPart = parts[0] + "--" + parts[1] + "--";
				String name = form.getFirstValue(inputName);
				String repository = form.getFirstValue(genericPart
						+ "repository");
				String key = form.getFirstValue(genericPart + "key");
				Package package_ = new Package(name, repository, key);
				for (Package p : packages) {
					if (package_.getName().equals(p.getName())) {
						throw (new ValidationException(
								"Cannot specify the same package multiply times: "
										+ package_.getName()));
					}
				}
				packages.add(package_);
			}
		}

		if (havePackagesChanged(packages)) {
			needsRebuild = true;
		}

		castToModule().setPackages(packages);

	}

	protected boolean havePackagesChanged(List<Package> packages) {
		boolean haveChanged = false;
		List<Package> oldPackages = castToModule().getPackages();
		if (packages.size() != oldPackages.size()) {
			haveChanged = true;
		} else {
			for (Package p : packages) {
				boolean foundIt = false;
				for (Package oldP : oldPackages) {
					if (p.equals(oldP)) {
						foundIt = true;
					}
				}
				if (!foundIt) {
					haveChanged = true;
				}
			}
		}
		return haveChanged;
	}

	private void parsePreRecipe(Form form) {

		String prerecipe = form.getFirstValue("prerecipe--script");
		if (prerecipe == null) {
			prerecipe = "";
		}

		if (!prerecipe.equals(castToModule().getPreRecipe())) {
			needsRebuild = true;
		}

		castToModule().setPreRecipe(prerecipe);

	}

	private void parseRecipe(Form form) {

		String recipe = form.getFirstValue("recipe--script");
		if (recipe == null) {
			recipe = "";
		}

		if (!recipe.equals(castToModule().getRecipe())) {
			needsRebuild = true;
		}

		castToModule().setRecipe(recipe);

	}

	private void parseTargets(Form form) throws ValidationException {

		List<Target> targets = new ArrayList<Target>();

		String[] targetNames = { "execute", "report" };

		for (String targetName : targetNames) {
			addTarget(form, targets, targetName);
		}

		castToModule().setTargets(targets);

	}

	private void addTarget(Form form, List<Target> targets, String targetName) {
		String target = form.getFirstValue(targetName + "--script");
		if (target != null) {
			targets.add(new Target(targetName, target));
		}
	}

	private void parseImageId(ImageModule module) throws ValidationException {
		if (needsRebuild) {
			return;
		}
		for (String cloudServiceName : ConnectorFactory.getCloudServiceNames()) {
			String imageId = getForm().getFirstValue(
					"cloudimageid_imageid_" + cloudServiceName);
			module.setImageId(imageId, cloudServiceName);
		}
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

	@Override
	public void adjustModule(Module previous) throws ValidationException {
		super.adjustModule(previous);

		ImageModule olderImage = (ImageModule) previous;
		ImageModule newImage = castToModule();
		if (!newImage.isBase()) {
			if (!needsRebuild) {
				for (CloudImageIdentifier cii : olderImage
						.getCloudImageIdentifiers()) {
					cii.copyTo(newImage);
				}
			}
		}
		if (newImage.getPlatform() == null) {
			newImage.setPlatform(olderImage.getPlatform());
		}
	}

	private ImageModule castToModule() {
		return (ImageModule) getParametrized();
	}

}
