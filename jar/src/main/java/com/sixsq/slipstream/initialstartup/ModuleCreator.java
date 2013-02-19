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
import com.sixsq.slipstream.persistence.User;

public class ModuleCreator {

	protected static final String PUBLIC_PROJECT_NAME = "Public";
	protected static final String BASE_IMAGES_PROJECT_NAME = PUBLIC_PROJECT_NAME
				+ "/BaseImages";
	protected static final String UBUNTU_PROJECT_NAME = BASE_IMAGES_PROJECT_NAME + "/Ubuntu";
	protected static final String UBUNTU_IMAGE_NAME = UBUNTU_PROJECT_NAME + "/12.04";

	protected static final String FEDORA_PROJECT_NAME = BASE_IMAGES_PROJECT_NAME + "/Fedora";
	protected static final String FEDORA_IMAGE_NAME = FEDORA_PROJECT_NAME + "/14.0";

	protected static User createUser(String name) {
		User user = null;
		try {
			user = new User(name);
		} catch (ValidationException e) {
			throw (new SlipStreamRuntimeException(e));
		}
		return user;
	}

}
