package com.sixsq.slipstream.connector.openstack;

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

import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ParameterType;

public class OpenStackUserParametersFactory extends UserParametersFactoryBase {

	public static String KEY_PARAMETER_NAME = "username";
	public static String SECRET_PARAMETER_NAME = "password";
	public static String TENANT_NAME = "tenant.name";
	public static String KEYSTONE_URL = "keystone.url";
	public static String KEYPAIR_NAME = "keypair.name";
	public static String SECURITY_GROUP = "security.group";
	public static String PRIVATE_KEY = "private.key";

	public OpenStackUserParametersFactory(String connectorInstanceName) throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		putParameter(KEY_PARAMETER_NAME, "", "Username", "", true);
		putPasswordParameter(SECRET_PARAMETER_NAME, "Password", true);
		putParameter(
				TENANT_NAME,
				"Project name (Correspond to your Keystone Tenant name for the project)",
				true);
		putParameter(KEYSTONE_URL, "",
				"Authentification URL (Correspond to the Keystone URL)", true);
		putParameter(KEYPAIR_NAME, "default", "Keypair Name", true);
		putParameter(SECURITY_GROUP, "default",
				"Security Groups (comma separated list)", true);
		putParameter(PRIVATE_KEY,
				"Private key of keypair (required to build Images)",
				ParameterType.Text, false);
	}

}
