package com.sixsq.slipstream.connector.local;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorBase;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.ClientExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.ClientHttpException;
import com.sixsq.slipstream.exceptions.SerializationException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

public class LocalConnector extends ConnectorBase {

	public static final String CLOUD_SERVICE_NAME = "local";
	public static final String CLOUDCONNECTOR_PYTHON_MODULENAME = "slipstream.cloudconnectors.dummy.DummyClientCloud";

	public LocalConnector() {
		this(CLOUD_SERVICE_NAME);
	}

	public LocalConnector(String instanceName) {
		super(instanceName != null ? instanceName : CLOUD_SERVICE_NAME);
	}

	public Connector copy(){
		return new LocalConnector(getConnectorInstanceName());
	}

	public String getCloudServiceName() {
		return CLOUD_SERVICE_NAME;
	}

	@Override
	public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate()
			throws ValidationException {
		return new LocalSystemConfigurationParametersFactory()
				.getParameters();
	}

	public void setCredentials(Credentials credentials) {
	}

	public void abort(Run run, User user) {
		return;
	}

	public Run launch(Run run, User user)
			throws ServerExecutionEnginePluginException,
			ClientExecutionEnginePluginException {

		Logger logger = Logger.getLogger(this.getClass().toString());
		try {

			logger.info("Element details:");
			logger.info("	Name: " + run.getRefqname());
			logger.info("	Description: " + run.getDescription());
			logger.info("	Category: " + run.getCategory());
			logger.info("	Type: " + run.getType());

			switch (run.getType()) {
			case Orchestration:
				launchDeployment(run);
				break;
			case Machine:
				launchBuild(run);
				break;
			case Run:
				launchImage(run);
				break;
			default:
				throw (new ServerExecutionEnginePluginException(
						"Cannot submit type: " + run.getCategory() + " yet!!"));
			}

			updateInstanceIdAndIpOnRun(run, "instance-id-for-local",
					"hostname-for-local");


		} catch (ClientHttpException e) {
			throw (new ClientExecutionEnginePluginException(e.getMessage()
					+ " (HTTP error: " + e.getStatus() + ")"));
		} catch (SlipStreamException e) {
			throw (new ServerExecutionEnginePluginException(
					"Error setting execution instance. Detail: "
							+ e.getMessage()));
		} catch (IOException e) {
			throw (new ServerExecutionEnginePluginException(
					"Error setting execution instance. Detail: "
							+ e.getMessage()));
		}

		return run;
	}

	private void launchDeployment(Run run) throws SlipStreamException,
			FileNotFoundException, IOException, SerializationException {
		String deploymentdir = "/tmp/slipstream/localexecution/"
				+ run.getName();
		boolean status = new File(deploymentdir).mkdirs();
		if (!status) {
			throw (new ServerExecutionEnginePluginException(
					"Error creating local resource: " + deploymentdir));
		}

		DeploymentModule deployment = (DeploymentModule) DeploymentModule
				.load(run.getRefqname());
		if (deployment.getNodes() == null) {
			// Empty deployment, nothing to
			throw (new SlipStreamClientException("Empty deployment, nothing to"));
		}

	}

	private void launchImage(Run run) {
		return;
	}

	private void launchBuild(Run run) {
		return;
	}

	@Override
	public Map<String, ModuleParameter> getImageParametersTemplate()
			throws ValidationException {
		return new LocalImageParametersFactory(getConnectorInstanceName()).getParameters();
	}

	@Override
	public Credentials getCredentials(User user) {
		return new LocalCredentials(user);
	}

	@Override
	public void terminate(Run run, User user)
			throws SlipStreamException {

		if (run.getType() != RunType.Orchestration) {
			Logger.getLogger(this.getClass().getName()).info("Terminating: " + run.getNodeNames());
			return;
		}
		
		String ids = run.getRuntimeParameterValue("orchestrator-local:hostname");

		if (ids == null) {
			return;
		}

        StringBuilder nodes = new StringBuilder(ids);

		for (String nodeName : run.getNodeNames().split(",")) {
			nodeName = nodeName.trim();
			if (!"".equals(nodeName)) {
                String multiplicity = run
                        .getParameterValue(
                                nodeName
                                        + RuntimeParameter.NODE_PROPERTY_SEPARATOR
                                        + RuntimeParameter.MULTIPLICITY_PARAMETER_NAME,
                                String.valueOf(RuntimeParameter.MULTIPLICITY_NODE_START_INDEX));
                for (int i = RuntimeParameter.MULTIPLICITY_NODE_START_INDEX; i <= Integer
                        .valueOf(multiplicity); i++) {
                    String ipKey = nodeName
                            + RuntimeParameter.NODE_PROPERTY_SEPARATOR
                            + RuntimeParameter.INSTANCE_ID_KEY;

                    nodes.append(" ").append(run.getRuntimeParameterValue(ipKey));
                }
			}
		}

		Logger.getLogger(this.getClass().getName()).info("Terminating: " + nodes.toString());

	}

	@Override
	public Properties describeInstances(User user) {
		return new Properties();
	}

	@Override
	public Map<String, UserParameter> getUserParametersTemplate()
			throws ValidationException{
		return new LocalUserParametersFactory().getParameters();
	}

	@Override
	protected String constructKey(String key) throws ValidationException {
		return new LocalUserParametersFactory().constructKey(key);
	}
}
