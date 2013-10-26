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

import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.User;

public abstract class RunFactory {

	public Run createRun(Module module, String cloudService, User user)
			throws SlipStreamClientException {
		return constructRun(module, RunType.Orchestration, cloudService, user);
	}

	protected Run createRun(Module module, RunType type, String cloudService,
			User user) throws SlipStreamClientException {
		return constructRun(module, type, cloudService, user);
	}

	public static Run getRun(Module module, String cloudService, User user)
			throws SlipStreamClientException {

		return getRun(module, RunType.Orchestration, cloudService, user);
	}

	public static Run getRun(Module module, RunType type, String cloudService,
			User user) throws SlipStreamClientException {
		RunFactory factory = selectFactory(type);

		Run run = factory.createRun(module, type, cloudService, user);

		return run;
	}

	static RunFactory selectFactory(RunType type)
			throws SlipStreamClientException {

		RunFactory factory = null;

		switch (type) {

		case Orchestration:
			factory = new DeploymentFactory();
			break;

		case Machine:
			factory = new BuildImageFactory();
			break;
		case Run:
			factory = new SimpleRunFactory();
			break;

		default:
			throw (new SlipStreamClientException("Unknown module type: "
					+ type));
		}
		return factory;
	}

	protected static Run constructRun(Module module, String cloudService,
			User user) throws ValidationException {
		return constructRun(module, RunType.Orchestration, cloudService, user);
	}

	protected static Run constructRun(Module module, RunType type,
			String cloudService, User user) throws ValidationException {
		return new Run(module, type, cloudService, user);
	}

	protected static void initOrchestratorNodeName(Run run) {
		run.addNodeName(Run.ORCHESTRATOR_NAME);
	}

	public static void terminate(Run run) {

	}

	public static void resolveImageIdIfAppropriate(Module module, User user)
			throws ConfigurationException, ValidationException {
		if (module != null && module.getCategory() == ModuleCategory.Image) {
			Connector connector = ConnectorFactory.getCurrentConnector(user);
			setImageId(module, connector);
		}
		if (module != null && module.getCategory() == ModuleCategory.Deployment) {
			for (Node n : ((DeploymentModule) module).getNodes().values()) {
				ImageModule imageModule = n.getImage();
				if (imageModule != null) {
					Connector connector = ConnectorFactory.getConnector(
							n.getCloudService(), user);
					setImageId(imageModule, connector);
				}
			}
		}
	}

	private static void setImageId(Module module, Connector connector)
			throws ValidationException {
		try {
			((ImageModule) module)
					.assignBaseImageIdToImageIdFromCloudService(connector
							.getConnectorInstanceName());
		} catch (ValidationException e) {
			// it's ok not to have an image id. Validation will handle this
			// later.
		}
	}

	/**
	 * Load the module corresponding to the run and set some of its attributes
	 * (e.g. multiplicity, cloud service) to match the specific configuration of
	 * the run.
	 * 
	 * The returned module is transient and not persisted.
	 */
	public abstract Module overloadModule(Run run, User user)
			throws ValidationException;

	protected Module loadModule(Run run) throws ValidationException {
		Module module = Module.load(run.getModuleResourceUrl());
		if (module == null) {
			throw new ValidationException("Unknown module: "
					+ run.getModuleResourceUrl());
		}
		return module;
	}

}