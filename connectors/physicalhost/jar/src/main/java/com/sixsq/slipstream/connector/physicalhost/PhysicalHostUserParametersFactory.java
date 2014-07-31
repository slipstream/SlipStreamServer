package com.sixsq.slipstream.connector.physicalhost;

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

public class PhysicalHostUserParametersFactory extends
		UserParametersFactoryBase {

	public static final String ORCHESTRATOR_USERNAME = "orchestrator.username";
	public static final String ORCHESTRATOR_PASSWORD = "orchestrator.password";
	public static final String ORCHESTRATOR_PRIVATE_KEY = "orchestrator.private.key";
	public static final String ORCHESTRATOR_HOST = "orchestrator.host";

	public PhysicalHostUserParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		putParameter(ORCHESTRATOR_USERNAME, "", "Orchestrator username", "",
				true);
		putParameter(
				ORCHESTRATOR_PASSWORD,
				"Orchestrator password",
				"You need to provide at least a password or a private key. If you provide a password and a private key, the password will be used as password for private key.",
				ParameterType.Password, false);
		putParameter(
				ORCHESTRATOR_PRIVATE_KEY,
				"Orchestrarot private key",
				"You need to provide at least a password or a private key. If you provide a password and a private key, the password will be used as password for private key.",
				ParameterType.Text, false);

		putMandatoryParameter(constructKey(ORCHESTRATOR_HOST),
				"Hostname or IP address of the machine where the orchestrator will be install.");
	}

}
