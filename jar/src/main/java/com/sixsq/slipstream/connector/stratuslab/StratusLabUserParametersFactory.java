package com.sixsq.slipstream.connector.stratuslab;

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

import java.util.Arrays;
import java.util.List;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.NetworkType;
import com.sixsq.slipstream.persistence.ParameterType;

public class StratusLabUserParametersFactory extends UserParametersFactoryBase {

	public static String KEY_PARAMETER_NAME = "username";
	public static String SECRET_PARAMETER_NAME = "password";

	public static String IP_TYPE_DEFAULT = NetworkType.Public.name()
			.toLowerCase();

	public StratusLabUserParametersFactory(String connectorInstanceName) throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {

		String[] _options = { IP_TYPE_DEFAULT, "local",
				NetworkType.Private.name().toLowerCase() };
		List<String> options = Arrays.asList(_options);
		putEnumParameter("ip.type", "IP type: public, local, private", options,
				IP_TYPE_DEFAULT, true);

		putParameter(KEY_PARAMETER_NAME, "StratusLab account username", true,
				ParameterType.RestrictedString);

		putPasswordParameter(SECRET_PARAMETER_NAME,
				"StratusLab account password", true);

		putParameter(
				ENDPOINT_KEY,
				Configuration
						.getInstance()
						.getParameters()
						.getParameterValue(
								constructKey("cloud.connector." + ENDPOINT_KEY),
								"cloud.lal.stratuslab.eu"),
				"StratusLab endpoint", true);

		putParameter(
				"marketplace.endpoint",
				Configuration
						.getInstance()
						.getParameters()
						.getParameterValue(
								constructKey("cloud.connector." + "marketplace.endpoint"),
								"http://marketplace.stratuslab.eu"),
				"Default marketplace endpoint", true);

		putParameter(SSHKEY_PARAMETER_NAME,
				"SSH Public Key(s) (keys must be separated by new line)",
				ParameterType.RestrictedText, true);
	}

}
