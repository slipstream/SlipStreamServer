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
import com.sixsq.slipstream.persistence.UserParameter;

public class ExecutionControlUserParametersFactory extends
		UserParametersFactoryBase {

	public static final String VERBOSITY_LEVEL = "Verbosity Level";
	public static final String VERBOSITY_LEVEL_DEFAULT = "0";
	public static final String CATEGORY = ParameterCategory.General.toString();

	public ExecutionControlUserParametersFactory() throws ValidationException {
		super(CATEGORY);
	}

	// Don't add quota parameter for this section
	@Override
	protected void initQuotaParameter()	throws ValidationException {
		return;
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {

		putMandatoryBooleanParameter(
				UserParameter.KEY_ON_ERROR_RUN_FOREVER,
				false,
				"If ANY error occurs, keep the execution running.",
				"This value is useful to investigate the error.",
				30);
		putMandatoryBooleanParameter(
				UserParameter.KEY_ON_SUCCESS_RUN_FOREVER,
				true,
				"If NO errors occur, keep the execution running.",
				"This value is useful for deployment or long tests.",
				20);
		putMandatoryParameter(
				UserParameter.KEY_TIMEOUT,
				"Execution timeout (in minutes).",
				"30",
				ParameterType.String,
				"If the execution stays in a transitional state for more than the value of this timeout, the execution is forcefully terminated.",
				90);
		putMandatoryParameter(
				SSHKEY_PARAMETER_NAME,
				"SSH Public Key(s) (one per line).",
				ParameterType.RestrictedText,
				"Warning: Some clouds may take into account only the first key until SlipStream bootstraps the machine.",
				40);

		String[] _options = { VERBOSITY_LEVEL_DEFAULT, "1", "2", "3" };
		List<String> options = Arrays.asList(_options);
		putMandatoryEnumParameter(
				VERBOSITY_LEVEL,
				"Level of verbosity.",
				options, VERBOSITY_LEVEL_DEFAULT,
				"0 - Actions,  1 - Steps,  2 - Details data,  3 - Debugging",
				99);

		List<String> clouds = extractCloudNames(ConnectorFactory.getConnectors());
		putMandatoryEnumParameter(
				UserParametersFactoryBase.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME,
				"Select the default cloud.",
				clouds, "",
				"Select the cloud that you want to use as the default.",
				10);

	}

}
