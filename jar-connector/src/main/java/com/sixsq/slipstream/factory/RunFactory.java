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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.CloudService;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorBase;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.ExecutionControlUserParametersFactory;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.Node;
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
import com.sixsq.slipstream.util.Logger;

public abstract class RunFactory {

	private static final List<String> VALID_RUNNING_STATE = Arrays.asList(
			"running", "on");
	private static final String RUNNING_STATE = "running";

	public Run createRun(Module module, String cloudService, User user)
			throws SlipStreamClientException {

		checkCloudServiceDefined(cloudService, user);

		validateModule(module, cloudService);

		Run run = constructRun(module, cloudService, user);

		validateRun(run, user, cloudService);

		initialize(module, run, user, cloudService);

		return run;
	}

	protected abstract Run constructRun(Module module, String cloudService,
			User user) throws ValidationException;

	protected void validateModule(Module module, String cloudService)
			throws SlipStreamClientException {

	}

	protected void validateRun(Run run, User user, String cloudService)
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

	private void checkCloudServiceDefined(String cloudService, User user)
			throws SlipStreamClientException {
		if ("".equals(cloudService)) {
			throw new SlipStreamClientException(
					ConnectorFactory
							.incompleteCloudConfigurationErrorMessage(user));
		}
	}

	protected void initialize(Module module, Run run, User user, String cloudService)
			throws ValidationException, NotFoundException {

		run = addOnSuccessRunForeverToParameters(run, user);
		run = addOnErrorRunForeverToParameters(run, user);

		run.setParameter(new RunParameter(Run.GARBAGE_COLLECTED_PARAMETER_NAME, "false",
				Run.GARBAGE_COLLECTED_PARAMETER_DESCRIPTION));

		initializeGlobalParameters(run);
		initCloudServices(run);

	}

	protected void initCloudServices(Run run) throws ValidationException {
		run.setCloudServiceNames(run.getCloudService());
	}

	public static Run getRun(Module module, RunType type, String cloudService,
			User user) throws SlipStreamClientException {
		return selectFactory(type).createRun(module, cloudService, user);
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

	protected static Run constructRun(Module module, RunType type,
			String cloudService, User user) throws ValidationException {
		return new Run(module, type, cloudService, user);
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

	// FIXME: move down to specific factories
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

	protected void initializeOrchestrtorRuntimeParameters(Run run)
			throws ValidationException {

		if (withOrchestrator(run)) {
			HashSet<String> cloudServiceList = getCloudServicesList(run);
			for (String cloudServiceName : cloudServiceList) {
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

		assignRuntimeParameters(run, prefix);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.HOSTNAME_KEY),
				RuntimeParameter.HOSTNAME_DESCRIPTION);
		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.INSTANCE_ID_KEY),
				RuntimeParameter.INSTANCE_ID_DESCRIPTION);

		run.getRuntimeParameters().get(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.IS_ORCHESTRATOR_KEY)).setValue("true");

		Configuration conf = Configuration.getInstance();
		String maxJaasWorkers = conf.getProperty(ServiceConfigurationParameter
				.constructKey(cloudService,
						RuntimeParameter.MAX_JAAS_WORKERS_KEY),
				RuntimeParameter.MAX_JAAS_WORKERS_DEFAULT);
		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.MAX_JAAS_WORKERS_KEY), maxJaasWorkers,
				RuntimeParameter.MAX_JAAS_WORKERS_DESCRIPTION);

		// Hack: hardcode the cpu and ram
		// need to get this from the connector?
		String defaultOrchestratorCpuRam = "1";
		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				Run.CPU_PARAMETER_NAME), defaultOrchestratorCpuRam,
				Run.CPU_PARAMETER_DESCRIPTION);
        run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
                Run.RAM_PARAMETER_NAME), defaultOrchestratorCpuRam,
                Run.RAM_PARAMETER_DESCRIPTION);
    }

	/**
	 * @param nodename
	 *            Example (< nodename>.< index>)
	 * @throws ValidationException
	 */
	public static void assignRuntimeParameters(Run run, String nodename)
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
				RuntimeParameter.COMPLETE_KEY),
				"false", RuntimeParameter.COMPLETE_DESCRIPTION);

        run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
        		RuntimeParameter.URL_SSH_KEY), "",
                RuntimeParameter.URL_SSH_DESCRIPTION);
        run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
        		RuntimeParameter.URL_SERVICE_KEY), "",
                RuntimeParameter.URL_SERVICE_DESCRIPTION);

        run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.IS_ORCHESTRATOR_KEY), "false",
				RuntimeParameter.IS_ORCHESTRATOR_DESCRIPTION);

		run.assignRuntimeParameter(RuntimeParameter.constructParamName(prefix,
				RuntimeParameter.SCALE_STATE_KEY),
				RuntimeParameter.SCALE_STATE_DEFAULT_VALUE,
				RuntimeParameter.SCALE_STATE_DESCRIPTION);
    }

	protected void initOrchestratorsNodeNames(Run run)
			throws ConfigurationException, ValidationException {
		HashSet<String> cloudServiceList = getCloudServicesList(run);
		if (withOrchestrator(run)){
			for (String cloudServiceName : cloudServiceList) {
				String nodename = Run.constructOrchestratorName(cloudServiceName);
				run.addNodeName(nodename, cloudServiceName);
				run.assignRuntimeParameter(nodename
						+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
						+ RuntimeParameter.CLOUD_SERVICE_NAME, cloudServiceName,
						RuntimeParameter.CLOUD_SERVICE_DESCRIPTION);
			}
		}
	}

	public static HashSet<String> getCloudServicesList(Run run)
			throws ValidationException {

		HashSet<String> cloudServicesList;

		if (run.getCategory() == ModuleCategory.Deployment) {
			cloudServicesList = DeploymentFactory.getCloudServicesList(run);
		} else {
			cloudServicesList = new HashSet<String>();
			cloudServicesList.add(run.getCloudService());
		}

		return cloudServicesList;
	}

	public static String getEffectiveCloudServiceName(String cloudService,
			Run run) {
		return CloudService.isDefaultCloudService(cloudService) ? run
				.getCloudService() : cloudService;
	}

	public static Properties describeInstances(User user, Run run)
			throws ValidationException {

		Properties describeInstancesStates = new Properties();

		String[] cloudServicesList = null;
		if (run != null) {
			cloudServicesList = run.getCloudServiceNameList();
		} else {
			cloudServicesList = ConnectorFactory.getCloudServiceNames();
		}

		for (String cloudServiceName : cloudServicesList) {
			Connector connector = ConnectorFactory
					.getConnector(cloudServiceName);

			Credentials credentials = connector.getCredentials(user);
			credentials.validate();

			Properties props;
			try {
				props = connector.describeInstances(user);
			} catch (SlipStreamException e) {
				Logger.warning(e.getMessage());
				continue;
			}
			for (String key : props.stringPropertyNames()) {
				describeInstancesStates.put(key, props.getProperty(key));
			}
		}
		return describeInstancesStates;
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

}
