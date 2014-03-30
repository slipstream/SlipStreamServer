package com.sixsq.slipstream.connector.cloudstack;

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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.CliConnectorBase;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.util.ProcessUtils;


public class CloudStackConnector extends CliConnectorBase {

	public static final String CLOUD_SERVICE_NAME = "cloudstack";
	public static final String CLOUDCONNECTOR_PYTHON_MODULENAME = "slipstream.cloudconnectors.cloudstack.CloudStackClientCloud";

	public CloudStackConnector() {
		this(CLOUD_SERVICE_NAME);
	}

    public CloudStackConnector(String instanceName) {
		super(instanceName);
	}

    public Connector copy(){
    	return new CloudStackConnector(getConnectorInstanceName());
    }

	public String getCloudServiceName() {
		return CLOUD_SERVICE_NAME;
	}

	@Override
	public Run launch(Run run, User user) throws SlipStreamException {

		if(isInOrchestrationContext(run) && run.getCategory() == ModuleCategory.Image)
			throw new SlipStreamException("Image creation is not yet available for this connector");

		String command;
		try {
			command = getRunInstanceCommand(run, user);
		} catch (IOException e) {
			throw (new SlipStreamException(
					"Failed getting run instance command", e));
		}

		String result;
		String[] commands = { "sh", "-c", command };
		try {
			result = ProcessUtils.execGetOutput(commands);
		} catch (IOException e) {
			e.printStackTrace();
			throw (new SlipStreamInternalException(e));
		} finally {
			deleteTempSshKeyFile();
		}

		String[] instanceData = parseRunInstanceResult(result);
		String instanceId = instanceData[0];
		String ipAddress = instanceData[1];

		updateInstanceIdAndIpOnRun(run, instanceId, ipAddress);

		return run;
	}

	private String getRunInstanceCommand(Run run, User user)
			throws InvalidElementException, ValidationException,
			SlipStreamClientException, IOException, ConfigurationException, ServerExecutionEnginePluginException {

		validateLaunch(run, user);

		String command = "/usr/bin/cloudstack-run-instances " +
				getCommandBaseParams(user) +
				" --image-id " + wrapInSingleQuotes(getImageId(run, user)) +
				" --instance-name " + wrapInSingleQuotes(getVmName(run)) +
				" --instance-type " + wrapInSingleQuotes(getInstanceType(run, user)) +
				" --public-key " + wrapInSingleQuotes(getPublicSshKey(run, user)) +
				" --network " + getNetwork(run) +
				" --context-script " + wrapInSingleQuotes(createContextualizationData(run, user));

		return command;
	}

	private String getCommandBaseParams(User user) throws ValidationException {
		return
			" --key " + getKey(user) +
			" --secret " + getSecret(user) +
			" --endpoint " + getEndpoint(user) +
			" --zone " + wrapInSingleQuotes(getZone(user));
	}

	protected String getVmName(Run run){
		return isInOrchestrationContext(run) ?
				getOrchestratorName(run) + "-" + run.getUuid() :
				"machine" + "-" + run.getUuid();
	}

	protected String getInstanceType(Run run, User user) throws ValidationException{
		return (isInOrchestrationContext(run)) ?
				user.getParameter(constructKey(CloudStackUserParametersFactory.ORCHESTRATOR_INSTANCE_TYPE_PARAMETER_NAME)).getValue() :
				getInstanceType( ImageModule.load(run.getModuleResourceUrl()) );
	}

	private void validateDescribe(User user) throws ValidationException {
		validateCredentials(user);
		validateBaseParameters(user);
	}

	private void validateTerminate(Run run, User user) throws ValidationException {
		validateCredentials(user);
		validateBaseParameters(user);
	}

	protected void validateBaseParameters(User user) throws ValidationException {
		String errorMessageLastPart = ". Please contact your SlipStream administrator.";

		String endpoint = getEndpoint(user);
		if (endpoint == null || "".equals(endpoint)) {
			throw (new ValidationException("Cloud Endpoint cannot be empty. "+ errorMessageLastPart));
		}
		String zone = getZone(user);
		if (zone == null || "".equals(zone)) {
			throw (new ValidationException("Cloud Zone cannot be empty. "+ errorMessageLastPart));
		}
	}

	private void validateLaunch(Run run, User user)
			throws ConfigurationException, SlipStreamClientException, ServerExecutionEnginePluginException{
		validateCredentials(user);
		validateBaseParameters(user);

		String instanceType = getInstanceType(run, user);
		if (instanceType == null || "".equals(instanceType)){
			if (isInOrchestrationContext(run)){
				throw (new ValidationException("Orchestrator instance type cannot be empty. Please contact your SlipStream administrator"));
			}else{
				throw (new ValidationException("Instance type cannot be empty. Please update your image parameters"));
			}
		}

		String imageId = getImageId(run, user);
		if (imageId == null  || "".equals(imageId)){
			if (isInOrchestrationContext(run)){
				throw (new ValidationException("Orchestrator image id cannot be empty. Please contact your SlipStream administrator"));
			}else{
				throw (new ValidationException("Image id cannot be empty. Please update your image parameters"));
			}
		}
	}

	protected String getNetwork(Run run) throws ValidationException{
		if (isInOrchestrationContext(run)) {
			return "Public";
		} else {
			ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
			return machine.getParameterValue(ImageModule.NETWORK_KEY, null);
		}
	}

	protected String getZone(User user) throws ValidationException {
		return user.getParameter(constructKey(
				CloudStackUserParametersFactory.ZONE_PARAMETER_NAME))
				.getValue();
	}

	@Override
	public void terminate(Run run, User user) throws SlipStreamException {

		validateTerminate(run, user);

		Logger.getLogger(this.getClass().getName()).info(
				getConnectorInstanceName() + ". Terminating all instances.");

		String command = "/usr/bin/cloudstack-terminate-instances"
				+ getCommandBaseParams(user);

        List<String> instanceIds = getCloudNodeInstanceIds(run);
		if(instanceIds.isEmpty()){
			throw new SlipStreamClientException("There is no instances to terminate");
		}

        StringBuilder instances = new StringBuilder();
        for (String id : instanceIds) {
            instances.append(" --instance-id ")
                     .append(wrapInSingleQuotes(id));
        }

        String[] commands = { "sh", "-c", command + instances.toString() };
		try {
			ProcessUtils.execGetOutput(commands);
		} catch (SlipStreamClientException e) {
			Logger.getLogger(this.getClass().getName()).info(
					getConnectorInstanceName() + ". Failed to terminate instances");
		} catch (IOException e) {
            Logger.getLogger(this.getClass().getName()).info(
                    getConnectorInstanceName() + ". IO error while terminating instances");
        }
	}

	@Override
	public Properties describeInstances(User user) throws SlipStreamException {

		validateDescribe(user);

		String command = "/usr/bin/cloudstack-describe-instances"
				+ getCommandBaseParams(user);

		String result;
		String[] commands = { "sh", "-c", command };

		try {
			result = ProcessUtils.execGetOutput(commands);
		} catch (IOException e) {
			e.printStackTrace();
			throw (new SlipStreamInternalException(e));
		}

		return parseDescribeInstanceResult(result);
	}

	@Override
	public Credentials getCredentials(User user) {
		return new CloudStackCredentials(user, getConnectorInstanceName());
	}

	@Override
	public Map<String, ModuleParameter> getImageParametersTemplate()
			throws ValidationException {
		return new CloudStackImageParametersFactory(getConnectorInstanceName())
				.getParameters();
	}

	@Override
	public Map<String, UserParameter> getUserParametersTemplate()
			throws ValidationException {
		return new CloudStackUserParametersFactory(getConnectorInstanceName())
				.getParameters();
	}

	@Override
	public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate()
			throws ValidationException {
		return new CloudStackSystemConfigurationParametersFactory(
				getConnectorInstanceName()).getParameters();
	}

	@Override
	protected String constructKey(String key) throws ValidationException {
		return new CloudStackUserParametersFactory(getConnectorInstanceName())
				.constructKey(key);
	}

	private String createContextualizationData(Run run, User user)
			throws ConfigurationException,
			ServerExecutionEnginePluginException, SlipStreamClientException {
		String logfilename = "orchestrator.slipstream.log";
		String bootstrap = "/tmp/slipstream.bootstrap";
		String username = user.getName();
		Configuration configuration = Configuration.getInstance();
		String targetScript = "";
		String nodename = Run.MACHINE_NAME;

		if(isInOrchestrationContext(run)){
			targetScript = "slipstream-orchestrator";
			nodename = getOrchestratorName(run);
		}

		String userData = "#!/bin/sh -e \n";
		userData += "# SlipStream contextualization script for CloudStack. \n";
		userData += "export SLIPSTREAM_CLOUD=\"" + getCloudServiceName() + "\"\n";
		userData += "export SLIPSTREAM_CONNECTOR_INSTANCE=\"" + getConnectorInstanceName() + "\"\n";
		userData += "export SLIPSTREAM_NODENAME=\"" + nodename + "\"\n";
		userData += "export SLIPSTREAM_DIID=\"" + run.getName() + "\"\n";
		userData += "export SLIPSTREAM_REPORT_DIR=\"" + SLIPSTREAM_REPORT_DIR + "\"\n";
		userData += "export SLIPSTREAM_SERVICEURL=\"" + configuration.baseUrl + "\"\n";
		userData += "export SLIPSTREAM_BUNDLE_URL=\"" + configuration.getRequiredProperty("slipstream.update.clienturl") + "\"\n";
		userData += "export SLIPSTREAM_BOOTSTRAP_BIN=\"" + configuration.getRequiredProperty("slipstream.update.clientbootstrapurl") + "\"\n";
		userData += "export CLOUDCONNECTOR_BUNDLE_URL=\"" + configuration.getRequiredProperty("cloud.connector.library.libcloud.url") + "\"\n";
		userData += "export CLOUDCONNECTOR_PYTHON_MODULENAME=\"" + CLOUDCONNECTOR_PYTHON_MODULENAME + "\"\n";
		userData += "export SLIPSTREAM_CATEGORY=\"" + run.getCategory().toString() + "\"\n";
		userData += "export SLIPSTREAM_USERNAME=\"" + username + "\"\n";
		userData += "export SLIPSTREAM_COOKIE=" + getCookieForEnvironmentVariable(username) + "\n";
		userData += "export SLIPSTREAM_VERBOSITY_LEVEL=\"" + getVerboseParameterValue(user) + "\"\n";

		userData += "mkdir -p " + SLIPSTREAM_REPORT_DIR + "\n"
				+ "wget --secure-protocol=SSLv3 --no-check-certificate -O " + bootstrap
				+ " $SLIPSTREAM_BOOTSTRAP_BIN > " + SLIPSTREAM_REPORT_DIR + "/"
				+ logfilename + " 2>&1 " + "&& chmod 0755 " + bootstrap + "\n"
				+ bootstrap + " " + targetScript + " >> "
				+ SLIPSTREAM_REPORT_DIR + "/" + logfilename + " 2>&1\n";

		return userData;
	}

}
