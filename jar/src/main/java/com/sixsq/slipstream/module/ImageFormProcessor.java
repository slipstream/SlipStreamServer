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
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Package;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.Target;
import com.sixsq.slipstream.persistence.User;

public class ImageFormProcessor extends ModuleFormProcessor {

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

		parseImageId(module);

		parsePackages(getForm());
		parsePreRecipe(getForm());
		parseRecipe(getForm());
		parseNewImage(getForm());
		parseTargets(getForm());
	}

	private void parseImageId(ImageModule module) throws ValidationException {
		if (!castToModule().isBase()) {
			return;
		}
		for (String cloudServiceName : ConnectorFactory.getCloudServiceNames()) {
			String imageId = getForm().getFirstValue(
					"cloudimageid_imageid_" + cloudServiceName);
			module.setImageId(imageId, cloudServiceName);
		}
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

		castToModule().setPackages(packages);

	}

	private void parsePreRecipe(Form form) {

		String prerecipe = form.getFirstValue("prerecipe--script");
		if (prerecipe == null) {
			prerecipe = "";
		}

		castToModule().setPreRecipe(prerecipe);

	}

	private void parseRecipe(Form form) {

		String recipe = form.getFirstValue("recipe--script");
		if (recipe == null) {
			recipe = "";
		}

		castToModule().setRecipe(recipe);

	}

	private void parseTargets(Form form) throws ValidationException {

		Set<String> formitems = form.getNames();

		// the targets are in the form:
		// - targets--[id]--name
		// - targets--[id]--script
		// - targets--[id]--runinbackground
		List<Target> targets = new ArrayList<Target>();

		for (String targetName : formitems.toArray(new String[0])) {
			if (targetName.startsWith("targets--")
					&& targetName.endsWith("--name")) {
				String[] bits = targetName.split("--");
				String genericPart = bits[0] + "--" + bits[1] + "--";
				String name = form.getFirstValue(targetName);
				String script = form.getFirstValue(genericPart + "script");
				Boolean runInBackground = "on".equals(form
						.getFirstValue(genericPart + "runinbackground")) ? true
						: false;
				Target target = new Target(name, script, runInBackground);
				for (Target t : targets) {
					if (target.getName().equals(t.getName())) {
						throw (new ValidationException(
								"Cannot have targets multiply defined: "
										+ target.getName()));
					}
				}
				targets.add(target);
			}
		}

		castToModule().setTargets(targets);

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
		if (platform == null) {
			platform = "";
		}

		castToModule().setPlatform(platform);

	}

	private ImageModule castToModule() {
		return (ImageModule) getParametrized();
	}

}
