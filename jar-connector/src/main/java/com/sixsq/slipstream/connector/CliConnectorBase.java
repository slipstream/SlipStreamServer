package com.sixsq.slipstream.connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;

public abstract class CliConnectorBase extends ConnectorBase {

	public static final String CLI_LOCATION = "/usr/bin";

	@Override
	abstract public String getCloudServiceName();

	@Override
	abstract public Run launch(Run run, User user) throws SlipStreamException;

	@Override
	abstract public Credentials getCredentials(User user);

	@Override
	abstract public void terminate(Run run, User user) throws SlipStreamException;

	@Override
	abstract public Properties describeInstances(User user) throws SlipStreamException;

	protected String getCloudConnectorBundleUrl(User user)
			throws ValidationException, ServerExecutionEnginePluginException {
		return getCloudParameterValue(user, UserParametersFactoryBase.UPDATE_CLIENTURL_PARAMETER_NAME);
	}

	/*abstract*/ protected String getCloudConnectorPythonModule(){return "";}//;

	protected Map<String, String> getCommandEnvironment(Run run, User user)
			throws ConfigurationException, ServerExecutionEnginePluginException, ValidationException {
		Map<String, String> environment = getCommonEnvironment(user);
		environment.putAll(getContextualizationEnvironment(run, user));
		environment.putAll(getCliParamsEnvironment(run, user));
		return environment;
	}


	private Map<String, String> getContextualizationEnvironment(Run run, User user)
			throws ConfigurationException, ServerExecutionEnginePluginException, ValidationException {

		Configuration configuration = Configuration.getInstance();
		String username = user.getName();
		String verbosityLevel = user
				.getParameterValue(
						new ExecutionControlUserParametersFactory()
								.constructKey(ExecutionControlUserParametersFactory.VERBOSITY_LEVEL),
				ExecutionControlUserParametersFactory.VERBOSITY_LEVEL_DEFAULT);

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
			throws ConfigurationException, ServerExecutionEnginePluginException, ValidationException {
		Map<String,String> environment = new HashMap<String,String>();

		environment.put("SLIPSTREAM_CONNECTOR_INSTANCE", getConnectorInstanceName());

		return environment;
	}

	private Map<String, String> getCliParamsEnvironment(Run run, User user)
			throws ConfigurationException, ServerExecutionEnginePluginException, ValidationException {
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

	public CliConnectorBase(String instanceName) {
		super(instanceName);
	}

	protected String getKey(User user) {
		return wrapInSingleQuotesOrNull(super.getKey(user));
	}

	protected String getSecret(User user) {
		return wrapInSingleQuotesOrNull(super.getSecret(user));
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

	protected String getEndpoint(User user) throws ValidationException {
		return wrapInSingleQuotes(super.getEndpoint(user));
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

}
