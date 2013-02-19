package com.sixsq.slipstream.initialstartup;

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

import com.sixsq.slipstream.connector.ParametersFactory;
import com.sixsq.slipstream.connector.stratuslab.StratusLabConnector;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.Platforms;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.User.State;

public class BaseImages extends ModuleCreator {

	private static final String SIXSQ = "sixsq";

	public static void create() throws ValidationException, NotFoundException,
			ConfigurationException {
		createUsers();
		createModules();
	}

	private static void createUsers() {

		if (User.loadByName(SIXSQ) != null) {
			return;
		}

		createSixSqUser();

	}

	private static void createSixSqUser() {
		User user = createUser(SIXSQ);
		user.setFirstName("SixSq");
		user.setLastName("Administrator");
		user.setEmail("slipstream-support@sixsq.com");
		user.setOrganization("SixSq");
		user.setPassword("siXsQsiXsQ");
		user.setState(State.ACTIVE);

		user.store();
	}

	private static void createModules() throws ValidationException,
			NotFoundException {

		User user = User.loadByName(SIXSQ);

		Module module;

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(BASE_IMAGES_PROJECT_NAME))) {
			module = new ProjectModule(PUBLIC_PROJECT_NAME);
			Authz authz = createPublicGetAuthz(user, module);
			authz.setInheritedGroupMembers(false);
			authz.setPublicCreateChildren(true);
			module.setAuthz(authz);
			module.store();

			module = new ProjectModule(BASE_IMAGES_PROJECT_NAME);
			module.setAuthz(createPublicGetAuthz(user, module));
			module.store();
		}

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(UBUNTU_PROJECT_NAME))) {
			module = new ProjectModule(UBUNTU_PROJECT_NAME);
			module.setAuthz(createPublicGetAuthz(user, module));
			module.store();

			ImageModule ubuntu = new ImageModule(UBUNTU_IMAGE_NAME);
			ubuntu.setAuthz(createPublicGetAuthz(user, module));
			ubuntu.setIsBase(true);
			ubuntu.getCloudImageIdentifiers().add(
					new CloudImageIdentifier(ubuntu, new StratusLabConnector()
							.getCloudServiceName(),
							"HZTKYZgX7XzSokCHMB60lS0wsiv"));
			ubuntu.setPlatform(Platforms.debian.toString());
			ubuntu.setLoginUser("root");
			ParametersFactory.addParametersForEditing(ubuntu);
			ubuntu.store();
		}

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(FEDORA_PROJECT_NAME))) {
			module = new ProjectModule(FEDORA_PROJECT_NAME);
			module.setAuthz(createPublicGetAuthz(user, module));
			module.store();

			ImageModule fedora = new ImageModule(FEDORA_IMAGE_NAME);
			fedora.setAuthz(createPublicGetAuthz(user, module));
			fedora.setIsBase(true);
			fedora.getCloudImageIdentifiers().add(
					new CloudImageIdentifier(fedora, new StratusLabConnector()
							.getCloudServiceName(),
							"ArCnG4gBogiprglBUdG6V8YX20y"));
			fedora.setPlatform(Platforms.redhat.toString());
			fedora.setLoginUser("root");
			ParametersFactory.addParametersForEditing(fedora);
			fedora.store();
		}
	}

	private static Authz createPublicGetAuthz(User user, Module module) {
		Authz authz = new Authz(user.getName(), module);
		authz.setPublicGet(true);
		return authz;
	}

}
