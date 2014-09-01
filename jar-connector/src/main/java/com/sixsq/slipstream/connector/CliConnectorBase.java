package com.sixsq.slipstream.connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ProcessException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.ProcessUtils;

public abstract class CliConnectorBase extends ConnectorBase {

	public static final String CLI_LOCATION = "/usr/bin";

	protected Logger log;

	@Override
	abstract public Credentials getCredentials(User user);

	@Override
	abstract public String getCloudServiceName();

	abstract protected String getCloudConnectorPythonModule();

	abstract protected Map<String, String> getConnectorSpecificUserParams(User user)
			throws ConfigurationException, ValidationException;

	abstract protected Map<String, String> getConnectorSpecificLaunchParams(Run run, User user)
			throws ConfigurationException, ValidationException;

	protected Map<String, String> getConnectorSpecificEnvironment(Run run, User user)
			throws ConfigurationException, ValidationException {
		return new HashMap<String, String>();
	}

	protected void validateLaunch(Run run, User user) throws ValidationException {
		validateCredentials(user);
	}

	protected void validateDescribe(User user) throws ValidationException {
		validateCredentials(user);
	}

	protected void validateTerminate(Run run, User user) throws ValidationException {
		validateCredentials(user);
	}

	protected String getCloudConnectorBundleUrl(User user) throws ValidationException {
		return getCloudParameterValue(user, UserParametersFactoryBase.UPDATE_CLIENTURL_PARAMETER_NAME);
	}

	public CliConnectorBase(String instanceName) {
		super(instanceName);
		this.log = Logger.getLogger(this.getClass().getName());
	}

	@Override
	public Run launch(Run run, User user) throws SlipStreamException {
		validateLaunch(run, user);

		String command = getCommandRunInstances() + createCliParameters(getLaunchParams(run, user));

		String result;
		String[] commands = { "sh", "-c", command };
		try {
			result = ProcessUtils.execGetOutput(commands, false, getCommandEnvironment(run, user));
		} catch (IOException e) {
			e.printStackTrace();
			throw (new SlipStreamInternalException(e));
		} catch (ProcessException e) {
			try {
				String[] instanceData = parseRunInstanceResult(e.getStdOut());
				updateInstanceIdAndIpOnRun(run, instanceData[0], instanceData[1]);
			} catch (Exception ex) { }
			throw e;
		} finally {
			deleteTempSshKeyFile();
		}

		String[] instanceData = parseRunInstanceResult(result);
		String instanceId = instanceData[0];
		String ipAddress = instanceData[1];

		updateInstanceIdAndIpOnRun(run, instanceId, ipAddress);

		return run;
	}

	@Override
	public Properties describeInstances(User user) throws SlipStreamException {
		validateCredentials(user);

		String command = getCommandDescribeInstances() + createCliParameters(getUserParams(user));

		String result;
		String[] commands = { "sh", "-c", command };

		try {
			result = ProcessUtils.execGetOutput(commands, getCommonEnvironment(user));
		} catch (IOException e) {
			e.printStackTrace();
			throw (new SlipStreamInternalException(e));
		}

		return parseDescribeInstanceResult(result);
	}

	@Override
	public void terminate(Run run, User user) throws SlipStreamException {

		validateCredentials(user);

		List<String> instanceIds = getCloudNodeInstanceIds(run);
		if(instanceIds.isEmpty()){
			throw new SlipStreamClientException("There is no instances to terminate");
		}

		StringBuilder instances = new StringBuilder();
        for (String id : instanceIds) {
            instances.append(" --instance-id ")
                     .append(wrapInSingleQuotes(id));
        }

		log.info(getConnectorInstanceName() + ". Terminating all instances.");

		String command = getCommandTerminateInstances() +
				createCliParameters(getUserParams(user)) +
				instances;

		String[] commands = { "sh", "-c", command};
		try {
			ProcessUtils.execGetOutput(commands, getCommonEnvironment(user));
		} catch (SlipStreamClientException e) {
			log.info(getConnectorInstanceName() + ". Failed to terminate instances");
		} catch (IOException e) {
			log.info(getConnectorInstanceName() + ". IO error while terminating instances");
		}
	}

	private String createCliParameters(Map<String, String> params) {
		String cliParams = "";
		for (Map.Entry<String, String> param: params.entrySet()) {
			cliParams += "--" + param.getKey() + " " + wrapInSingleQuotes(param.getValue()) + " ";
		}
		return cliParams;
	}

	private Map<String, String> getLaunchParams(Run run, User user)
			throws ConfigurationException, SlipStreamClientException {
		Map<String, String> launchParams = new HashMap<String, String>();
		launchParams.putAll(getUserParams(user));
		launchParams.putAll(getGenericLaunchParams(run, user));
		launchParams.putAll(getConnectorSpecificLaunchParams(run, user));
		return launchParams;
	}

	private Map<String, String> getGenericLaunchParams(Run run, User user)
			throws ConfigurationException, SlipStreamClientException {
		Map<String, String> launchParams = new HashMap<String, String>();
		launchParams.put("image-id", getImageId(run, user));
		launchParams.put("network-type", getNetwork(run));
		return launchParams;
	}

	private Map<String, String> getUserParams(User user)
			throws ValidationException {
		Map<String, String> userParams = new HashMap<String, String>();
		userParams.putAll(getGenericUserParams(user));
		userParams.putAll(getConnectorSpecificUserParams(user));
		return userParams;
	}

	protected Map<String, String> getGenericUserParams(User user) {
		Map<String, String> userParams = new HashMap<String, String>();
		userParams.put(UserParametersFactoryBase.KEY_PARAMETER_NAME, getKey(user));
		userParams.put(UserParametersFactoryBase.SECRET_PARAMETER_NAME, getSecret(user));
		return userParams;
	}

	private String getVerbosityLevel(User user) throws ValidationException {
		String key = ExecutionControlUserParametersFactory.VERBOSITY_LEVEL;
		String qualifiedKey = new ExecutionControlUserParametersFactory().constructKey(key);
		String defaultValue = ExecutionControlUserParametersFactory.VERBOSITY_LEVEL_DEFAULT;
		return user.getParameterValue(qualifiedKey, defaultValue);
	}

	protected Map<String, String> getCommandEnvironment(Run run, User user)
			throws ConfigurationException, ValidationException {
		Map<String, String> environment = getCommonEnvironment(user);
		environment.putAll(getContextualizationEnvironment(run, user));
		environment.putAll(getCliParamsEnvironment(run, user));
		environment.putAll(getConnectorSpecificEnvironment(run, user));
		return environment;
	}


	private Map<String, String> getContextualizationEnvironment(Run run, User user)
			throws ConfigurationException, ValidationException {

		Configuration configuration = Configuration.getInstance();
		String username = user.getName();
		String verbosityLevel = getVerbosityLevel(user);

		String nodeInstanceName = (isInOrchestrationContext(run)) ? getOrchestratorName(run) : Run.MACHINE_NAME;

		Map<String,String> environment = new HashMap<String,String>();

		environment.put("SLIPSTREAM_DIID", run.getName());
		environment.put("SLIPSTREAM_SERVICEURL", configuration.baseUrl);
		environment.put("SLIPSTREAM_NODE_INSTANCE_NAME", nodeInstanceName);
		environment.put("SLIPSTREAM_REPORT_DIR", SLIPSTREAM_REPORT_DIR);
		environment.put("SLIPSTREAM_CLOUD", getCloudServiceName());
		environment.put("SLIPSTREAM_BUNDLE_URL", configuration.getRequiredProperty("slipstream.update.clienturl"));
		environment.put("SLIPSTREAM_BOOTSTRAP_BIN", configuration.getRequiredProperty("slipstream.update.clientbootstrapurl"));

		environment.put("SLIPSTREAM_USERNAME", username);
		environment.put("SLIPSTREAM_COOKIE", generateCookie(username, run.getUuid()));
		environment.put("SLIPSTREAM_VERBOSITY_LEVEL", verbosityLevel);

		environment.put("CLOUDCONNECTOR_BUNDLE_URL", getCloudConnectorBundleUrl(user));
		environment.put("CLOUDCONNECTOR_PYTHON_MODULENAME", getCloudConnectorPythonModule());

		return environment;
	}

	protected Map<String, String> getCommonEnvironment(User user)
			throws ConfigurationException, ValidationException {
		Map<String,String> environment = new HashMap<String,String>();

		environment.put("SLIPSTREAM_CONNECTOR_INSTANCE", getConnectorInstanceName());

		return environment;
	}

	private Map<String, String> getCliParamsEnvironment(Run run, User user)
			throws ConfigurationException, ValidationException {
		String isOrchestrator = (isInOrchestrationContext(run)) ? "True" : "False";
		String publicSshKey;

		try {
			publicSshKey = getPublicSshKey(run, user);
		} catch (IOException e) {
			throw new ValidationException(e.getMessage());
		}

		Map<String,String> environment = new HashMap<String,String>();

		environment.put("__SLIPSTREAM_SSH_PUB_KEY", publicSshKey);
		environment.put("IS_ORCHESTRATOR", isOrchestrator);

		return environment;
	}

	public static Properties parseDescribeInstanceResult(String result)
			throws SlipStreamException {
		Properties states = new Properties();
		List<String> lines = new ArrayList<String>();
		Collections.addAll(lines, result.split("\n"));
		lines.remove(0);
		for (String line : lines) {
			String[] parts = line.trim().split("\\s+");
			if (parts.length < 2) {
				throw (new SlipStreamException(
						"Error returned by describe command. Got: " + result));
			}
			String instanceIdKey = parts[0];
			String status = parts[1];
			states.put(instanceIdKey, status);
		}
		return states;
	}

	public static String[] parseRunInstanceResult(String result)
			throws SlipStreamClientException {
		String[] lines = result.split("\n");
		if (lines.length < 1) {
			throw (new SlipStreamClientException(
					"Error returned by launch command. Got: " + result));
		}
		String line = lines[lines.length-1];
		String[] parts = line.trim().split(",");
		if (parts.length != 2) {
			throw (new SlipStreamClientException(
					"Error returned by launch command. Got: " + result));
		}
		return parts;
	}

	protected String wrapInSingleQuotesOrNull(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		} else {
			return wrapInSingleQuotes(value);
		}
	}

	protected String wrapInSingleQuotes(String value) {
		return "'" + value.replaceAll("'","'\\\\''") + "'";
	}

	protected String getNetwork(Run run) throws ValidationException {
		if (isInOrchestrationContext(run)) {
			return "Public";
		} else {
			ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
			return machine.getParameterValue(ImageModule.NETWORK_KEY, null);
		}
	}

	protected String getErrorMessageLastPart(User user) {
		return ", please edit your <a href='/user/" + user.getName()
				+ "'> user account</a>";
	}

	protected void validateCredentials(User user) throws ValidationException {
		String errorMessageLastPart = getErrorMessageLastPart(user);

		if (getKey(user) == null) {
			throw (new ValidationException(
					"Cloud Username cannot be empty"
							+ errorMessageLastPart));
		}
		if (getSecret(user) == null) {
			throw (new ValidationException(
					"Cloud Password cannot be empty"
							+ errorMessageLastPart));
		}
	}

	public String getCliLocation() {
		return CLI_LOCATION;
	}

	private String getCommandGenericPart(){
		return getCliLocation() + "/" + getCloudServiceName();
	}

	protected String getCommandRunInstances() {
		return getCommandGenericPart() + "-run-instances ";
	}

	protected String getCommandTerminateInstances() {
		return getCommandGenericPart() + "-terminate-instances ";
	}

	protected String getCommandDescribeInstances() {
		return getCommandGenericPart() + "-describe-instances ";
	}




}
