package com.sixsq.slipstream.factory;

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

import java.util.Map;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunParameter;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;

public class SimpleRunFactory extends BuildImageFactory {

	@Override
	protected RunType getRunType() {
		return RunType.Run;
	}

	@Override
	protected void validateModule(Module module, Map<String, String> cloudServicePerNode)
	        throws SlipStreamClientException {

		ImageModule image = (ImageModule) module;

		checkNoCircularDependencies(image);

		String cloudServiceName = cloudServicePerNode.get(nodeInstanceName);
		image.validateForRun(cloudServiceName);
	}

	@Override
	protected void initExtraRunParameters(Module module, Run run) throws ValidationException {
		ImageModule image = (ImageModule) module;

		String cloudService = run.getParameterValue(constructParamName(
				nodeInstanceName,
				RuntimeParameter.CLOUD_SERVICE_NAME), CloudImageIdentifier.DEFAULT_CLOUD_SERVICE);

		boolean runBuildRecipes = image.hasToRunBuildRecipes(cloudService);

		String runParameterName = constructParamName(nodeInstanceName, RunParameter.NODE_RUN_BUILD_RECIPES_KEY);
		run.setParameter(new RunParameter(runParameterName, String.valueOf(runBuildRecipes),
				RunParameter.NODE_RUN_BUILD_RECIPES_DESCRIPTION));
	}

}
