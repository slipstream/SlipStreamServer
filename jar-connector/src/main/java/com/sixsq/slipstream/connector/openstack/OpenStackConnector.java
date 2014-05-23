package com.sixsq.slipstream.connector.openstack;

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
import com.sixsq.slipstream.connector.CliConnectorBase;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.*;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.util.ProcessUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;


public class OpenStackConnector extends CliConnectorBase {

	private static Logger log = Logger.getLogger(OpenStackConnector.class.toString());

	public static final String CLOUD_SERVICE_NAME = "openstack";
	public static final String CLOUDCONNECTOR_PYTHON_MODULENAME = "slipstream.cloudconnectors.openstack.OpenStackClientCloud";

	public OpenStackConnector() {
		this(CLOUD_SERVICE_NAME);
	}

	public OpenStackConnector(String instanceName){
		super(instanceName != null ? instanceName : CLOUD_SERVICE_NAME);
	}

	public Connector copy(){
		return new OpenStackConnector(getConnectorInstanceName());
	}

	public String getCloudServiceName() {
		return CLOUD_SERVICE_NAME;
	}

	@Override
	public Run launch(Run run, User user) throws SlipStreamException {

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
			result = ProcessUtils.execGetOutput(commands, false);
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

	private String getRunInstanceCommand(Run run, User user)
			throws InvalidElementException, ValidationException,
			SlipStreamClientException, IOException, ConfigurationException, ServerExecutionEnginePluginException {

		validate(run, user);

		String command = "/usr/bin/openstack-run-instances " +
				getCommandUserParams(user) +
				" --instance-type " + wrapInSingleQuotes(getInstanceType(run)) +
				" --image-id " + wrapInSingleQuotes(getImageId(run, user)) +
				" --instance-name " + wrapInSingleQuotes(getVmName(run)) +
				" --network-type " + getNetwork(run) +
				" --security-groups " + wrapInSingleQuotes(getSecurityGroups(run)) +
				" --public-key " + wrapInSingleQuotes(getPublicSshKey(run, user)) +
		 		" --context-script " + wrapInSingleQuotes(createContextualizationData(run, user));

		return command;
	}

	private String getCommandUserParams(User user) throws ValidationException {
		validate(user);
		return
		" --username " + getKey(user) +
		" --password " + getSecret(user) +
		" --project " + wrapInSingleQuotes(getProject(user)) +
		" --endpoint " + getEndpoint(user) +
		" --region " + wrapInSingleQuotes(getRegion()) +
		" --service-type " + wrapInSingleQuotes(getServiceType()) +
		" --service-name " + wrapInSingleQuotes(getServiceName());

	}

	protected String getServiceType() throws ConfigurationException, ValidationException {
		return Configuration.getInstance().getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_TYPE_PARAMETER_NAME));
	}

	protected String getServiceName() throws ConfigurationException, ValidationException {
		return Configuration.getInstance().getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_NAME_PARAMETER_NAME));
	}

	protected String getRegion() throws ConfigurationException, ValidationException {
		return Configuration.getInstance().getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_REGION_PARAMETER_NAME));
	}

	protected String getVmName(Run run) {
		return run.getType() == RunType.Orchestration ? getOrchestratorName(run)
				+ "-" + run.getUuid()
				: "machine" + "-" + run.getUuid();
	}

	protected String getInstanceType(Run run)
			throws SlipStreamClientException, ConfigurationException {
		return (isInOrchestrationContext(run)) ? Configuration.getInstance()
				.getRequiredProperty(constructKey(OpenStackUserParametersFactory.ORCHESTRATOR_INSTANCE_TYPE_PARAMETER_NAME))
				: getInstanceType(ImageModule.load(run.getModuleResourceUrl()));
	}

	protected String getNetwork(Run run) throws ValidationException{
		if (run.getType() == RunType.Orchestration) {
			return "";
		} else {
			ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
			return machine.getParameterValue(ImageModule.NETWORK_KEY, null);
		}
	}

	protected String getSecurityGroups(Run run) throws ValidationException{
		return (isInOrchestrationContext(run)) ? "default"
				: getParameterValue(OpenStackImageParametersFactory.SECURITY_GROUPS, ImageModule.load(run.getModuleResourceUrl()));
	}

	protected String getProject(User user) throws ValidationException {
		return user.getParameter(constructKey(
				OpenStackUserParametersFactory.TENANT_NAME)).getValue(null);
	}

	private void validate(User user) throws ValidationException {
		validateCredentials(user);
	}

	private void validate(Run run, User user)
			throws ConfigurationException, SlipStreamClientException, ServerExecutionEnginePluginException {
		validateRun(run, user);
	}

	protected void validateCredentials(User user) throws ValidationException {
		super.validateCredentials(user);

		String endpoint = getEndpoint(user);
		if (endpoint == null || "".equals(endpoint)) {
			throw (new ValidationException("Cloud Endpoint cannot be empty. Please contact your SlipStream administrator."));
		}
	}

	private void validateRun(Run run, User user)
			throws ConfigurationException, SlipStreamClientException, ServerExecutionEnginePluginException{

		String instanceSize = getInstanceType(run);
		if (instanceSize == null || instanceSize.isEmpty() || "".equals(instanceSize) ){
				throw (new ValidationException("Instance type cannot be empty."));
		}

		String imageId = getImageId(run, user);
		if (imageId == null  || "".equals(imageId)){
			throw (new ValidationException("Image ID cannot be empty"));
		}

	}

	@Override
	public void terminate(Run run, User user) throws SlipStreamException {

		validateCredentials(user);

		Logger.getLogger(this.getClass().getName()).info(
				getConnectorInstanceName() + ". Terminating all instances.");

		String command = "/usr/bin/openstack-terminate-instances"
				+ getCommandUserParams(user);

		for (String id : getCloudNodeInstanceIds(run)) {
			String[] commands = { "sh", "-c", command +
					" --instance-id " + wrapInSingleQuotes(id) };
			try {
				ProcessUtils.execGetOutput(commands);
			} catch (SlipStreamClientException e) {
				Logger.getLogger(this.getClass().getName()).info(
						getConnectorInstanceName() + ". Failed to terminate instance " + id);
			} catch (IOException e) { }
		}
	}

	@Override
	public Properties describeInstances(User user) throws SlipStreamException {

		validateCredentials(user);

		String command = "/usr/bin/openstack-describe-instances"
				+ getCommandUserParams(user);

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
	public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate()
			throws ValidationException {
		return new OpenStackSystemConfigurationParametersFactory(getConnectorInstanceName())
				.getParameters();
	}

	@Override
	public Map<String, UserParameter> getUserParametersTemplate()
			throws ValidationException {
		return new OpenStackUserParametersFactory(getConnectorInstanceName()).getParameters();
	}

	@Override
	public Map<String, ModuleParameter> getImageParametersTemplate()
			throws ValidationException {
		return new OpenStackImageParametersFactory(getConnectorInstanceName()).getParameters();
	}

	@Override
	protected String constructKey(String key) throws ValidationException {
		return new OpenStackUserParametersFactory(getConnectorInstanceName()).constructKey(key);
	}

	@Override
	public Credentials getCredentials(User user) {
		return new OpenStackCredentials(user, getConnectorInstanceName());
	}

	protected String createContextualizationData(Run run, User user) throws ConfigurationException,
			ServerExecutionEnginePluginException, SlipStreamClientException {

		Configuration configuration = Configuration.getInstance();

		String logfilename = "orchestrator.slipstream.log";
		String bootstrap = "/tmp/slipstream.bootstrap";
		String username = user.getName();

		String targetScript = "";
		String nodename = Run.MACHINE_NAME;
        if(isInOrchestrationContext(run)){
			targetScript = "slipstream-orchestrator";
			nodename = getOrchestratorName(run);
		}

		String userData = "#!/bin/sh -e \n";
		userData += "# SlipStream contextualization script for VMs on Amazon. \n";
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
		userData += "export OPENSTACK_SERVICE_TYPE=\"" + configuration.getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_TYPE_PARAMETER_NAME)) + "\"\n";
		userData += "export OPENSTACK_SERVICE_NAME=\"" + configuration.getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_NAME_PARAMETER_NAME)) + "\"\n";
		userData += "export OPENSTACK_SERVICE_REGION=\"" + configuration.getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_REGION_PARAMETER_NAME)) + "\"\n";
		userData += "export SLIPSTREAM_CATEGORY=\"" + run.getCategory().toString() + "\"\n";
		userData += "export SLIPSTREAM_USERNAME=\"" + username + "\"\n";
		userData += "export SLIPSTREAM_COOKIE=" + getCookieForEnvironmentVariable(username) + "\n";
		userData += "export SLIPSTREAM_VERBOSITY_LEVEL=\"" + getVerboseParameterValue(user) + "\"\n";

		/*userData += "mkdir -p ~/.ssh\n"
				+ "echo '" + getPublicSshKey(run, user) + "' >> ~/.ssh/authorized_keys\n"
				+ "chmod 0700 ~/.ssh\n"
				+ "chmod 0640 ~/.ssh/authorized_keys\n";
		*/
		userData += "mkdir -p " + SLIPSTREAM_REPORT_DIR + "\n"
				+ "wget --secure-protocol=SSLv3 --no-check-certificate -O " + bootstrap
				+ " $SLIPSTREAM_BOOTSTRAP_BIN > " + SLIPSTREAM_REPORT_DIR + "/"
				+ logfilename + " 2>&1 " + "&& chmod 0755 " + bootstrap + "\n"
				+ bootstrap + " " + targetScript + " >> "
				+ SLIPSTREAM_REPORT_DIR + "/" + logfilename + " 2>&1\n";

		//System.out.print(userData);

		return userData;
	}

}
