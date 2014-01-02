package com.sixsq.slipstream.run;

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

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.User;

public class SimpleRunFactory extends BuildImageFactory {

	@Override
	protected Run constructRun(Module module, String cloudService, User user)
			throws ValidationException {
		return new Run(module, RunType.Run, cloudService, user);
	}

	@Override
	protected void initialize(Module module, Run run, String cloudService)
			throws ValidationException, NotFoundException {

		initializeGlobalParameters(run);
		initRuntimeParameters(module, run);
		initMachineState(run);
		initNodeNames(run, cloudService);
	}

	@Override
	protected void validateModule(Module module, String cloudService)
			throws SlipStreamClientException {

		ImageModule image = (ImageModule) module;

		checkNoCircularDependencies(image);

		image.validateForRun(cloudService);
	}
	
	private static void initNodeNames(Run run, String cloudService) {
		run.addNodeName(Run.MACHINE_NAME);
		run.addGroup(Run.MACHINE_NAME, cloudService);
	}

	@Override
	public Module overloadModule(Run run, User user) throws ValidationException {
		Module module = loadModule(run);
		return ImageModule.populateForImageRun(run, module);
	}
}
