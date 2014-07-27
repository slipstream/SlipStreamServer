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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.ConnectorBase;
import com.sixsq.slipstream.connector.ExecutionControlUserParametersFactory;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunParameter;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.util.FileUtil;

public abstract class RunFactory {

	private static final List<String> VALID_RUNNING_STATE = Arrays.asList("running", "on");
	private static final String RUNNING_STATE = "running";


	public final Run createRun(Module module, User user) throws SlipStreamClientException {
		return createRun(module, user, null);
	}

	public final Run createRun(Module module, User user,
			Map<String, List<Parameter<?>>> userChoices) throws SlipStreamClientException {

		validateModuleForRun(module);

		Map<String, String> cloudServicePerNode = resolveCloudServiceNames(module, user, userChoices);
		Set<String> cloudServiceNames = new HashSet<String>(cloudServicePerNode.values());

		validateModule(module, cloudServicePerNode);

		Run run = new Run(module, getRunType(), cloudServiceNames, user);

		initCloudServices(run, cloudServicePerNode);

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

	private void validateModuleForRun(Module module) throws ValidationException {
		try {
			castToRequiredModuleType(module);
		} catch (ClassCastException e) {
			throw new ValidationException("Module validation failed: " + e.getMessage());
		}
	}

	private void initDefaultRunParameters(Run run, User user) throws ValidationException {
		run = addOnSuccessRunForeverToParameters(run, user);
		run = addOnErrorRunForeverToParameters(run, user);

		run.setParameter(new RunParameter(Run.GARBAGE_COLLECTED_PARAMETER_NAME, "false",
				Run.GARBAGE_COLLECTED_PARAMETER_DESCRIPTION));
	}

	private void initialize(Module module, Run run, User user)
			throws ValidationException, NotFoundException {

		initializeGlobalParameters(run);

		init(module, run, user);

		initializeOrchestratorRuntimeParameters(run);
		initOrchestratorsNodeNames(run);

	}

	protected abstract RunType getRunType();

	protected abstract void init(Module module, Run run, User user)
			throws ValidationException, NotFoundException;

	protected abstract Map<String, String> resolveCloudServiceNames(Module module, User user,
			Map<String, List<Parameter<?>>> userChoices);

	protected abstract void addUserFormParametersAsRunParameters(Module module, Run run,
			Map<String, List<Parameter<?>>> userChoices) throws ValidationException;

	protected abstract Module castToRequiredModuleType(Module module);

	private void initCloudServices(Run run, Map<String, String> cloudServicePerNode) throws ValidationException {
		for (Map.Entry<String, String> entry: cloudServicePerNode.entrySet()) {
			String key = constructParamName(entry.getKey(), RuntimeParameter.CLOUD_SERVICE_NAME);
			RunParameter rp = new RunParameter(key, entry.getValue(), RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);
			run.setParameter(rp);
		}
	}

	protected abstract void initExtraRunParameters(Module module, Run run) throws ValidationException;

	protected abstract void updateExtraRunParameters(Module module, Run run,
			Map<String, List<Parameter<?>>> userChoices) throws ValidationException;

	protected void validateModule(Module module, Map<String, String> cloudServicePerNode)
	        throws SlipStreamClientException {
		return;
	}

	protected void validateRun(Run run, User user)
			throws SlipStreamClientException {

		validatePublicSshKey(run, user);
	}

	private void validatePublicSshKey(Run run, User user)
			throws ValidationException {
		if (run.getType() != RunType.Run) {
			String publicSshKeyFile = Configuration.getInstance().getProperty(
					ServiceConfiguration.CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY, null);

			if (publicSshKeyFile == null || "".equals(publicSshKeyFile)) {
				throw new ValidationException(
						"The path to the SSH public key to put in the orchestrator is empty. "
								+ "Please contact your SlipStream administrator.");
			}
			if (!FileUtil.exist(publicSshKeyFile)) {
				throw new ValidationException(
						"The path to the SSH public key to put in the orchestrator points to a nonexistent file. "
								+ "Please contact your SlipStream administrator.");
			}
		}

		String publicSshKey = user
				.getParameterValue(
						ExecutionControlUserParametersFactory.CATEGORY
								+ "."
								+ UserParametersFactoryBase.SSHKEY_PARAMETER_NAME,
						null);
		if (publicSshKey == null || "".equals(publicSshKey)) {
			throw new ValidationException(
					"Missing public key in your user profile.");
		}
	}

	public static Run getRun(Module module, RunType type, User user)
			throws SlipStreamClientException {
		return getRun(module, type, user, null);
	}

	public static Run getRun(Module module, RunType type, User user, Map<String, List<Parameter<?>>> userChoices)
			throws SlipStreamClientException {
		if (userChoices == null) {
			userChoices = new HashMap<String, List<Parameter<?>>>();
		}
		return selectFactory(type).createRun(module, user, userChoices);
	}

	public static RunFactory selectFactory(RunType type)
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
			throw (new SlipStreamClientException("Unknown module type: " + type));
		}
		return factory;
	}

	private Run addOnSuccessRunForeverToParameters(Run run, User user) throws ValidationException {
		String key = Parameter.constructKey(ExecutionControlUserParametersFactory.CATEGORY,
				UserParameter.KEY_ON_SUCCESS_RUN_FOREVER);

		UserParameter up = user.getParameter(key);
		if (up != null) {
			run.setParameter(new RunParameter(up.getName(), up.getValue("false"), up.getDescription()));
		}

		return run;
	}

	private Run addOnErrorRunForeverToParameters(Run run, User user) throws ValidationException {
		String key = Parameter.constructKey(ExecutionControlUserParametersFactory.CATEGORY,
				UserParameter.KEY_ON_ERROR_RUN_FOREVER);

		UserParameter up = user.getParameter(key);
		if (up != null) {
			run.setParameter(new RunParameter(up.getName(), up.getValue(), up.getDescription()));
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

	protected void initializeOrchestratorRuntimeParameters(Run run)
			throws ValidationException {

		if (withOrchestrator(run)) {
			for (String cloudServiceName : getCloudServiceNames(run)) {
				initializeOrchestratorParameters(run, cloudServiceName);
			}
		}
	}

	private static boolean withOrchestrator(Run run) {
		return ConnectorBase.isInOrchestrationContext(run);
	}

	protected static void initializeGlobalParameters(Run run)
			throws ValidationException {

		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_CATEGORY_KEY, run
				.getCategory().toString(), "Module category");

		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_ABORT_KEY, "",
				RuntimeParameter.GLOBAL_ABORT_DESCRIPTION);
		run.assignRuntimeParameter(RuntimeParameter.GLOBAL_STATE_KEY,
				Run.INITIAL_NODE_STATE,
				RuntimeParameter.GLOBAL_STATE_DESCRIPTION);
        run.assignRuntimeParameter(RuntimeParameter.GLOBAL_NODE_GROUPS_KEY, "",
                RuntimeParameter.GLOBAL_NODE_GROUPS_DESCRIPTION);

        run.assignRuntimeParameter(RuntimeParameter.GLOBAL_URL_SERVICE_KEY, "",
                RuntimeParameter.GLOBAL_URL_SERVICE_DESCRIPTION);

        run.assignRuntimeParameter(RuntimeParameter.GLOBAL_TAGS_KEY, "",
				RuntimeParameter.GLOBAL_TAGS_DESCRIPTION);

        run.assignRuntimeParameter(RuntimeParameter.GLOBAL_RECOVERY_MODE_KEY, "false",
				RuntimeParameter.GLOBAL_RECOVERY_MDDE_DESCRIPTION);
	}

	private static void initializeOrchestratorParameters(Run run,
			String cloudService) throws ValidationException {

		String prefix = Run.constructOrchestratorName(cloudService);

		assignCommonRuntimeParameters(run, prefix);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.HOSTNAME_KEY),
				RuntimeParameter.HOSTNAME_DESCRIPTION);
		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.INSTANCE_ID_KEY),
				RuntimeParameter.INSTANCE_ID_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.IS_ORCHESTRATOR_KEY), "true", RuntimeParameter.IS_ORCHESTRATOR_DESCRIPTION);

		Configuration conf = Configuration.getInstance();
		String maxJaasWorkers = conf.getProperty(ServiceConfigurationParameter
				.constructKey(cloudService,
						RuntimeParameter.MAX_JAAS_WORKERS_KEY),
				RuntimeParameter.MAX_JAAS_WORKERS_DEFAULT);
		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.MAX_JAAS_WORKERS_KEY), maxJaasWorkers,
				RuntimeParameter.MAX_JAAS_WORKERS_DESCRIPTION);
    }

	/**
	 * @param nodename
	 *            Example (< nodename>.< index>)
	 * @throws ValidationException
	 */
	public static void assignCommonRuntimeParameters(Run run, String nodename)
			throws ValidationException {
		String prefix = nodename;

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.STATE_CUSTOM_KEY), "",
				RuntimeParameter.STATE_CUSTOM_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.STATE_VM_KEY), "",
				RuntimeParameter.STATE_VM_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.ABORT_KEY), "",
				RuntimeParameter.ABORT_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.COMPLETE_KEY), "false",
				RuntimeParameter.COMPLETE_DESCRIPTION);

        run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
        		RuntimeParameter.URL_SSH_KEY), "",
                RuntimeParameter.URL_SSH_DESCRIPTION);

        run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
        		RuntimeParameter.URL_SERVICE_KEY), "",
                RuntimeParameter.URL_SERVICE_DESCRIPTION);

    }

	protected static void assignCommonNodeRuntimeParameters(Run run, String nodename)
			throws ValidationException {

		assignCommonRuntimeParameters(run, nodename);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(nodename,
				RuntimeParameter.IS_ORCHESTRATOR_KEY), "false",
				RuntimeParameter.IS_ORCHESTRATOR_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(nodename,
				RuntimeParameter.SCALE_STATE_KEY),
				RuntimeParameter.SCALE_STATE_DEFAULT_VALUE,
				RuntimeParameter.SCALE_STATE_DESCRIPTION);

	}

	protected void initOrchestratorsNodeNames(Run run)
			throws ConfigurationException, ValidationException {
		if (withOrchestrator(run)){
			for (String cloudServiceName : getCloudServiceNames(run)) {
				String nodename = Run.constructOrchestratorName(cloudServiceName);
				run.addNodeInstanceName(nodename, cloudServiceName);
				run.assignRuntimeParameter(nodename
						+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
						+ RuntimeParameter.CLOUD_SERVICE_NAME, cloudServiceName,
						RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);
			}
		}
	}

	public static List<String> getCloudServiceNames(Run run)
			throws ValidationException {
		return run.getCloudServiceNamesList();
	}

	public static Run updateVmStatus(Run run, User user)
			throws SlipStreamException {
		List<Vm> vms = Vm.list(user.getName());
		run = populateVmStateProperties(run, vms);
		return run;
	}

	public static Run updateVmStatus(Run run, List<Vm> vms)
			throws SlipStreamException {
		run = populateVmStateProperties(run, vms);
		return run;
	}

	public static Run populateVmStateProperties(Run run,
			List<Vm> vms) throws NotFoundException,
			ValidationException {

		List<String> nodes = run.getNodeNameList();
		String vmIdKey;
		String vmId;
		String vmStateKey;

		Map<String, Vm> map = Vm.toMap(vms);

		for (String nodeName : nodes) {
			String keyPrefix = nodeName
					+ RuntimeParameter.NODE_PROPERTY_SEPARATOR;
			vmIdKey = keyPrefix + RuntimeParameter.INSTANCE_ID_KEY;
			vmId = run.getRuntimeParameterValueIgnoreAbort(vmIdKey);
			vmId = vmId == null ? "" : vmId;
			vmStateKey = keyPrefix + RuntimeParameter.STATE_VM_KEY;
			Vm vm = map.get(vmId);
			String vmState = vm == null ? "Unknown" : vm.getState();

			vmState = cleanVmState(vmState);

			try {
				run.updateRuntimeParameter(vmStateKey, vmState);
			} catch (NotFoundException e) {
				run.assignRuntimeParameter(vmStateKey, vmState,
						RuntimeParameter.STATE_VM_DESCRIPTION);
			}
		}

		return run;
	}

	private static String cleanVmState(String vmState) {
		if (vmState != null) {
			if (VALID_RUNNING_STATE.contains(vmState.toLowerCase())) {
				vmState = RUNNING_STATE;
			}
		}
		return vmState;
	}

	public static String constructParamName(String nodename, String paramname) {
		return RuntimeParameter.constructParamName(nodename, paramname);
	}

}
