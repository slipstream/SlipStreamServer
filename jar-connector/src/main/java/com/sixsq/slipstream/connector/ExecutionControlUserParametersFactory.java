package com.sixsq.slipstream.connector;

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

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ParameterType;

public class ExecutionControlUserParametersFactory extends
		UserParametersFactoryBase {

	public static final String VERBOSITY_LEVEL = "Verbosity Level";
	public static final String VERBOSITY_LEVEL_DEFAULT = "0";
	public static final String CATEGORY = ParameterCategory.General.toString();

	public ExecutionControlUserParametersFactory() throws ValidationException {
		super(CATEGORY);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {

		putParameter(
				"On Error Run Forever",
				true,
				"If an error occurs, keep the execution running for investigation.",
				true);
		putParameter(
				"On Success Run Forever",
				false,
				"If no errors occur, keep the execution running. Useful for deployment or long tests.",
				true);
		putParameter(
				"Timeout",
				"30",
				"Minutes - When this timeout is reached, the execution is forcefully terminated.",
				true);
		putMandatoryParameter(SSHKEY_PARAMETER_NAME,
				"SSH Public Key(s) (keys must be separated by new line) Warning: Some clouds may take into account only the first key.",
				ParameterType.RestrictedText);
		String[] _options = {VERBOSITY_LEVEL_DEFAULT, "1", "2", "3"};
		List<String> options = Arrays.asList(_options);
		putEnumParameter(
				VERBOSITY_LEVEL,
				"Level of verbosity. 0 - Actions, 1 - Steps, 2 - Details data, 3 - Debugging.", 
				options, VERBOSITY_LEVEL_DEFAULT, true);

		List<String> clouds = extractCloudNames(ConnectorFactory.getConnectors());
		putEnumParameter(
				UserParametersFactoryBase.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME,
				"Select which cloud you want to use.", 
				clouds, "", true);
	
	}

}
