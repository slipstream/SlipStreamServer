package com.sixsq.slipstream.connector.okeanos;

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

import com.sixsq.slipstream.connector.openstack.OpenStackUserParametersFactory;
import com.sixsq.slipstream.exceptions.ValidationException;

public class OkeanosUserParametersFactory extends OpenStackUserParametersFactory {

	public OkeanosUserParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		putMandatoryParameter(KEY_PARAMETER_NAME, "Username");
		putMandatoryPasswordParameter(SECRET_PARAMETER_NAME, "Password");
		putMandatoryParameter(TENANT_NAME,
				"Project name (sometime it's called Tenant name)");
	}

}
