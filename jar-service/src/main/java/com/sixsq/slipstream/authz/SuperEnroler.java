package com.sixsq.slipstream.authz;

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

import org.restlet.Application;
import org.restlet.data.ClientInfo;
import org.restlet.security.Enroler;
import org.restlet.security.Role;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;

public class SuperEnroler implements Enroler {

	public static Role Super;

	public SuperEnroler(Application application) {
		Super = new Role(application,
				"Privileged user (i.e. administrator)");
	}

	public void enrole(ClientInfo clientInfo) {
		User user = null;
		try {
			user = User.loadByNameNoParams(clientInfo.getUser().getIdentifier());
		} catch (ConfigurationException e) {
			Util.throwConfigurationException(e);
		}
		if (user != null && user.isSuper()) {
			clientInfo.getRoles().add(Super);
		}

	}

}
