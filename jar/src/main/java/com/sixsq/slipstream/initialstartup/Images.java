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

import static com.sixsq.slipstream.initialstartup.Users.SIXSQ;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

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

public class Images extends ModuleCreator {

	public static void create() throws ValidationException, NotFoundException,
			ConfigurationException, NoSuchAlgorithmException,
			UnsupportedEncodingException {

		assert User.loadByName(SIXSQ) != null;
		createModules();
	}

	private static void createModules() throws ValidationException,
			NotFoundException {

		User user = User.loadByName(SIXSQ);

		Module module;

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(EXAMPLES_PROJECT_NAME))) {
			module = new ProjectModule(EXAMPLES_PROJECT_NAME);
			Authz authz = createPublicGetAuthz(user, module);
			authz.setInheritedGroupMembers(false);
			authz.setPublicCreateChildren(true);
			module.setAuthz(authz);
			module.store();
		}

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(IMAGES_PROJECT_NAME))) {
			module = new ProjectModule(IMAGES_PROJECT_NAME);
			module.setAuthz(createPublicGetAuthz(user, module));
			module.store();
		}

		if (!ImageModule.exists(ImageModule
				.constructResourceUri(UBUNTU_IMAGE_NAME))) {

			ImageModule ubuntu = new ImageModule(UBUNTU_IMAGE_NAME);
			ubuntu.setAuthz(createPublicGetAuthz(user, ubuntu));
			ubuntu.setIsBase(true);
			ubuntu.getCloudImageIdentifiers().add(
					new CloudImageIdentifier(ubuntu, new StratusLabConnector()
							.getCloudServiceName(),
							"KBhcU87Wm5IZNOXZYGHrczGekwp"));
			ubuntu.setPlatform(Platforms.ubuntu.toString());
			ubuntu.setLoginUser("root");
			ParametersFactory.addParametersForEditing(ubuntu);
			ubuntu.store();
		}

		if (!ImageModule.exists(ImageModule
				.constructResourceUri(CENTOS_IMAGE_NAME))) {

			ImageModule centos = new ImageModule(CENTOS_IMAGE_NAME);
			centos.setAuthz(createPublicGetAuthz(user, centos));
			centos.setIsBase(true);
			centos.getCloudImageIdentifiers().add(
					new CloudImageIdentifier(centos, new StratusLabConnector()
							.getCloudServiceName(),
							"H8dg0ssw_j4jg67FTwXysCUrJPl"));
			centos.setPlatform(Platforms.redhat.toString());
			centos.setLoginUser("root");
			ParametersFactory.addParametersForEditing(centos);
			centos.store();
		}
	}

}
