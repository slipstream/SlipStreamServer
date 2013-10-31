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

import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.User;

public class ModuleCreator {

	protected static final String EXAMPLES_PROJECT_NAME = "examples";

	protected static final String IMAGES_PROJECT_NAME = EXAMPLES_PROJECT_NAME
			+ "/images";
	protected static final String UBUNTU_IMAGE_NAME = IMAGES_PROJECT_NAME
			+ "/ubuntu-12.04";
	protected static final String CENTOS_IMAGE_NAME = IMAGES_PROJECT_NAME
			+ "/centos-6.3";

	protected static final String TUTORIALS_PROJECT_NAME = EXAMPLES_PROJECT_NAME
			+ "/tutorials";

	protected static final String RSTUDIO_PROJECT_NAME = TUTORIALS_PROJECT_NAME
			+ "/rstudio";
	protected static final String RSTUDIO_IMAGE_NAME = RSTUDIO_PROJECT_NAME
			+ "/rstudio-appliance";
	protected static final String RSTUDIO_DEPLOYMENT_NAME = RSTUDIO_PROJECT_NAME
			+ "/rstudio";

	protected static final String TORQUE_PROJECT_NAME = TUTORIALS_PROJECT_NAME
			+ "/torque";
	protected static final String TORQUE_MASTER_IMAGE_NAME = TORQUE_PROJECT_NAME
			+ "/torque-master";
	protected static final String TORQUE_WORKER_IMAGE_NAME = TORQUE_PROJECT_NAME
			+ "/torque-worker";
	protected static final String TORQUE_DEPLOYMENT_NAME = TORQUE_PROJECT_NAME
			+ "/torque";

	protected static final String SERVICE_TESTING_PROJECT_NAME = TUTORIALS_PROJECT_NAME
			+ "/service-testing";
	protected static final String APACHE_IMAGE_NAME = SERVICE_TESTING_PROJECT_NAME
			+ "/apache";
	protected static final String CLIENT_IMAGE_NAME = SERVICE_TESTING_PROJECT_NAME
			+ "/client";
	protected static final String SERVICE_TESTING_DEPLOYMENT_NAME = SERVICE_TESTING_PROJECT_NAME
			+ "/system";


	protected static User createUser(String name) {
		User user = null;
		try {
			user = new User(name);
		} catch (ValidationException e) {
			throw (new SlipStreamRuntimeException(e));
		}
		return user;
	}

	protected static Authz createPublicGetAuthz(User user, Module module) {
		Authz authz = new Authz(user.getName(), module);
		authz.setPublicGet(true);
		return authz;
	}

}
