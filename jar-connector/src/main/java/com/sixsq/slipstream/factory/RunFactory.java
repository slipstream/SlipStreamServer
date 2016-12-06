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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.ConnectorBase;
import com.sixsq.slipstream.connector.ExecutionControlUserParametersFactory;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunParameter;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.util.FileUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class RunFactory {

	public final Run createRun(Module module, User user) throws SlipStreamClientException {
		return createRun(module, user, null);
	}

	public final Run createRun(Module module, User user, Map<String, List<Parameter<?>>> userChoices)
			throws SlipStreamClientException {

		validateModuleForRun(module);

		Map<String, String> cloudServicePerNode = resolveCloudServiceNames(module, user, userChoices);
		Set<String> cloudServiceNames = new HashSet<String>(cloudServicePerNode.values());

		Map<String, String> instanceTypePerNode = resolveInstanceTypes(module, user, userChoices);
		validateModule(module, cloudServicePerNode);

		Run run = new Run(module, getRunType(), cloudServiceNames, user);

		initCloudServices(run, cloudServicePerNode);
		initInstanceTypes(run, instanceTypePerNode);

		initDefaultRunParameters(run, user);
		initExtraRunParameters(module, run);
		if (userChoices != null && !userChoices.isEmpty()) {
			addUserFormParametersAsRunParameters(module, run, userChoices);
			updateExtraRunParameters(module, run, userChoices);
		}

		validateRun(run, user);

		initialize(module, run, user);

		return run;
	}

	protected void validateModuleForRun(Module module) throws ValidationException {
		try {
			castToRequiredModuleType(module);
		} catch (ClassCastException e) {
			throw new ValidationException("Module validation failed: " + e.getMessage());
		}
	}

	private void initDefaultRunParameters(Run run, User user) throws ValidationException {
		run = addDefaultKeepRunningToParameters(run, user);

		run.setParameter(new RunParameter(Run.GARBAGE_COLLECTED_PARAMETER_NAME, "false",
				Run.GARBAGE_COLLECTED_PARAMETER_DESCRIPTION));
	}

	private void initialize(Module module, Run run, User user) throws ValidationException, NotFoundException {

		initializeGlobalParameters(run);

		init(module, run, user);

		initializeOrchestratorRuntimeParameters(run);
		initOrchestratorsNodeNames(run);

	}

	protected abstract RunType getRunType();

	protected abstract void init(Module module, Run run, User user) throws ValidationException, NotFoundException;

	protected abstract Map<String, String> resolveInstanceTypes(Module module, User user,
																	Map<String, List<Parameter<?>>> userChoices);

	protected abstract Map<String, String> resolveCloudServiceNames(Module module, User user,
			Map<String, List<Parameter<?>>> userChoices);

	protected abstract void addUserFormParametersAsRunParameters(Module module, Run run,
			Map<String, List<Parameter<?>>> userChoices) throws ValidationException;

	protected abstract Module castToRequiredModuleType(Module module);

	private void initCloudServices(Run run, Map<String, String> cloudServicePerNode) throws ValidationException {
		for (Map.Entry<String, String> entry : cloudServicePerNode.entrySet()) {
			String keyCloudService = constructParamName(entry.getKey(), RuntimeParameter.CLOUD_SERVICE_NAME);
			RunParameter rpCloudService = new RunParameter(keyCloudService, entry.getValue(), RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);
			run.setParameter(rpCloudService);
		}
	}

	private void initInstanceTypes(Run run, Map<String, String> instanceTypePerNode) throws ValidationException {
		for (Map.Entry<String, String> entry : instanceTypePerNode.entrySet()) {
			String keyInstanceType = constructParamName(entry.getKey(), RuntimeParameter.INSTANCE_TYPE_KEY);
			RunParameter rpInstanceType = new RunParameter(keyInstanceType, entry.getValue(), RuntimeParameter.INSTANCE_TYPE_DESCRIPTION);
			run.setParameter(rpInstanceType);
		}
	}

	protected abstract void initExtraRunParameters(Module module, Run run) throws ValidationException;

	protected abstract void updateExtraRunParameters(Module module, Run run, Map<String, List<Parameter<?>>> userChoices)
			throws ValidationException;

	protected void validateModule(Module module, Map<String, String> cloudServicePerNode)
			throws SlipStreamClientException {
		return;
	}

	protected void validateRun(Run run, User user) throws SlipStreamClientException {

		validateCloudServicesSet(run);
		validatePublicSshKey(run, user);
	}

	private void validateCloudServicesSet(Run run) throws ValidationException {
		if (run.getCloudServiceNamesList().length == 0) {
			throw new ValidationException("No cloud service names set on the run.");
		}
	}

	private void validatePublicSshKey(Run run, User user) throws ValidationException {
		if (run.getType() != RunType.Run) {
			String publicSshKeyFile = Configuration.getInstance().getProperty(
					ServiceConfiguration.CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY, null);

			if (publicSshKeyFile == null || "".equals(publicSshKeyFile)) {
				throw new ValidationException("The path to the SSH public key to put in the orchestrator is empty. "
						+ "Please contact your SlipStream administrator.");
			}
			if (!FileUtil.exist(publicSshKeyFile)) {
				throw new ValidationException(
						"The path to the SSH public key to put in the orchestrator points to a nonexistent file. "
								+ "Please contact your SlipStream administrator.");
			}
		}
	}

	public static void validateUserPublicSshKeys(User user) throws ValidationException {
		String publicSshKey = user.getParameterValue(ExecutionControlUserParametersFactory.CATEGORY + "."
				+ UserParametersFactoryBase.SSHKEY_PARAMETER_NAME, null);

		if (publicSshKey == null || publicSshKey.trim().isEmpty()) {
			throw new ValidationException("Missing public key in your user profile.");
		}
	}

	public static Run getRun(Module module, RunType type, User user) throws SlipStreamClientException {
		return getRun(module, type, user, null);
	}

	public static Run getRun(Module module, RunType type, User user, Map<String, List<Parameter<?>>> userChoices)
			throws SlipStreamClientException {
		if (userChoices == null) {
			userChoices = new HashMap<String, List<Parameter<?>>>();
		}
		return selectFactory(type).createRun(module, user, userChoices);
	}

	public static RunFactory selectFactory(RunType type) throws SlipStreamClientException {

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
			throw (new SlipStreamClientException("Unknown module type: " + type));
		}
		return factory;
	}

	private Run addDefaultKeepRunningToParameters(Run run, User user) throws ValidationException {
		String key = Parameter.constructKey(ExecutionControlUserParametersFactory.CATEGORY,
				UserParameter.KEY_KEEP_RUNNING);

		UserParameter up = user.getParameter(key);
		if (up != null) {
			run.setParameter(new RunParameter(up.getName(), up.getValue(), up.getDescription()));
		} else {
			throw new ValidationException("Parameter 'Keep running after deployment' not found in the user profile.");
		}

		return run;
	}

	public static void terminate(Run run) {

	}

	public static Module loadModule(Run run) throws ValidationException {
		Module module = Module.load(run.getModuleResourceUrl());
		if (module == null) {
			throw new ValidationException("Unknown module: " + run.getModuleResourceUrl());
		}
		return module;
	}

	protected void initializeOrchestratorRuntimeParameters(Run run) throws ValidationException {

		if (withOrchestrator(run)) {
			for (String cloudServiceName : getCloudServiceNames(run)) {
				initializeOrchestratorParameters(run, cloudServiceName);
			}
		}
	}

	private static boolean withOrchestrator(Run run) {
		return ConnectorBase.isInOrchestrationContext(run);
	}

	protected static void initializeGlobalParameters(Run run) throws ValidationException {

		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_CATEGORY_KEY, run.getCategory().toString(),
				"Module category");

		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_COMPLETE_KEY, "",
				RuntimeParameter.GLOBAL_COMPLETE_DESCRIPTION);
		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_ABORT_KEY, "", RuntimeParameter.GLOBAL_ABORT_DESCRIPTION);
		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_STATE_KEY, Run.INITIAL_NODE_STATE,
				RuntimeParameter.GLOBAL_STATE_DESCRIPTION);
		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_NODE_GROUPS_KEY, "",
				RuntimeParameter.GLOBAL_NODE_GROUPS_DESCRIPTION).setIsSet(true);

		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_URL_SERVICE_KEY, "",
				RuntimeParameter.GLOBAL_URL_SERVICE_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_TAGS_KEY, "", RuntimeParameter.GLOBAL_TAGS_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_RECOVERY_MODE_KEY, "false",
				RuntimeParameter.GLOBAL_RECOVERY_MDDE_DESCRIPTION);
	}

	private static void initializeOrchestratorParameters(Run run, String cloudService) throws ValidationException {

		if (cloudService.isEmpty()) {
			throw new ValidationException("Failed to intialise orchestrator parameters: empty cloud service name.");
		}

		String prefix = Run.constructOrchestratorName(cloudService);

		assignCommonRuntimeParameters(run, prefix);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.HOSTNAME_KEY),
				RuntimeParameter.HOSTNAME_DESCRIPTION);
		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.INSTANCE_ID_KEY),
				RuntimeParameter.INSTANCE_ID_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.IS_ORCHESTRATOR_KEY),
				"true", RuntimeParameter.IS_ORCHESTRATOR_DESCRIPTION);

		Configuration conf = Configuration.getInstance();
		String maxJaasWorkers = conf.getProperty(
				ServiceConfigurationParameter.constructKey(cloudService, RuntimeParameter.MAX_JAAS_WORKERS_KEY),
				RuntimeParameter.MAX_JAAS_WORKERS_DEFAULT);
		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.MAX_JAAS_WORKERS_KEY),
				maxJaasWorkers, RuntimeParameter.MAX_JAAS_WORKERS_DESCRIPTION);
	}

	/**
	 * @param nodename
	 *            Example (< nodename>.< index>)
	 * @throws ValidationException
	 */
	public static void assignCommonRuntimeParameters(Run run, String nodename) throws ValidationException {
		String prefix = nodename;

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.STATE_CUSTOM_KEY), "",
				RuntimeParameter.STATE_CUSTOM_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.STATE_VM_KEY), "",
				RuntimeParameter.STATE_VM_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.ABORT_KEY), "",
				RuntimeParameter.ABORT_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.COMPLETE_KEY), "false",
				RuntimeParameter.COMPLETE_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.URL_SSH_KEY), "",
				RuntimeParameter.URL_SSH_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix, RuntimeParameter.URL_SERVICE_KEY), "",
				RuntimeParameter.URL_SERVICE_DESCRIPTION);

	}

	protected static void assignCommonNodeInstanceRuntimeParameters(Run run, String nodename)
			throws ValidationException {

		assignCommonRuntimeParameters(run, nodename);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(nodename, RuntimeParameter.IS_ORCHESTRATOR_KEY),
				"false", RuntimeParameter.IS_ORCHESTRATOR_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(nodename, RuntimeParameter.SCALE_STATE_KEY),
				RuntimeParameter.SCALE_STATE_DEFAULT_VALUE, RuntimeParameter.SCALE_STATE_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(nodename, RuntimeParameter.PRE_SCALE_DONE_KEY),
				RuntimeParameter.PRE_SCALE_DONE_DEFAULT_VALUE, RuntimeParameter.PRE_SCALE_DONE_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(nodename, RuntimeParameter.SCALE_IAAS_DONE_KEY),
				RuntimeParameter.SCALE_IAAS_DONE_DEFAULT_VALUE, RuntimeParameter.SCALE_IAAS_DONE_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(nodename, RuntimeParameter.SCALE_DISK_ATTACH_SIZE_KEY),
				RuntimeParameter.SCALE_DISK_ATTACH_SIZE_DEFAULT_VALUE, RuntimeParameter.SCALE_DISK_ATTACH_SIZE_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(nodename, RuntimeParameter.SCALE_DISK_ATTACHED_DEVICE_KEY),
				RuntimeParameter.SCALE_DISK_ATTACHED_DEVICE_DEFAULT_VALUE, RuntimeParameter.SCALE_DISK_ATTACHED_DEVICE_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(nodename, RuntimeParameter.SCALE_DISK_DETACH_DEVICE_KEY),
				RuntimeParameter.SCALE_DISK_DETACH_DEVICE_DEFAULT_VALUE, RuntimeParameter.SCALE_DISK_DETACH_DEVICE_DESCRIPTION);

	}

	protected void initOrchestratorsNodeNames(Run run) throws ConfigurationException, ValidationException {
		if (withOrchestrator(run)) {
			for (String cloudServiceName : getCloudServiceNames(run)) {
				String nodename = Run.constructOrchestratorName(cloudServiceName);
				run.addNodeInstanceName(nodename, cloudServiceName);
				run.assignRuntimeParameter(nodename + RuntimeParameter.NODE_PROPERTY_SEPARATOR
						+ RuntimeParameter.CLOUD_SERVICE_NAME, cloudServiceName,
						RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);
			}
		}
	}

	public static String[] getCloudServiceNames(Run run) throws ValidationException {
		return run.getCloudServiceNamesList();
	}

	public static String constructParamName(String nodename, String paramname) {
		return RuntimeParameter.constructParamName(nodename, paramname);
	}

	protected static void findAndAddImagesApplicationParameters(Map<String, ModuleParameter> parameters,
																ImageModule image)
	{
		for (Map.Entry<String, ModuleParameter> entry : image.getParameters().entrySet()) {
			ModuleParameter parameter = entry.getValue();
			if (ParameterCategory.applicationParameters().contains(parameter.getCategory())) {
				parameters.putIfAbsent(entry.getKey(), parameter);
			}
		}

		ImageModule parent = image.getParentModule();
		if (parent != null) {
			findAndAddImagesApplicationParameters(parameters, parent);
		}
	}

}
