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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.ConnectorBase;
import com.sixsq.slipstream.connector.Credentials;
import com.sixsq.slipstream.connector.stratuslab.StratusLabSystemConfigurationParametersFactory;
import com.sixsq.slipstream.exceptions.ClientExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.ClientHttpException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SerializationException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunStatus;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

public class LocalConnector extends ConnectorBase {

	public static final String CLOUD_SERVICE_NAME = "local";
	
	public LocalConnector() {
		this(CLOUD_SERVICE_NAME);
	}
	
	public LocalConnector(String instanceName) {
		super(instanceName);
	}
	
	public String getCloudServiceName() {
		return CLOUD_SERVICE_NAME;
	}

	@Override
	protected String getOrchestratorImageId() throws ConfigurationException, ValidationException {
		return Configuration.getInstance().getRequiredProperty(
				constructKey("orchestrator.imageid"));
	}

	@Override
	public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate()
			throws ValidationException {
		return new StratusLabSystemConfigurationParametersFactory(getConnectorInstanceName())
				.getParameters();
	}

	public void setCredentials(Credentials credentials) {
	}

	public void abort(Run run, User user) {
		return;
	}

	public RunStatus getStatus(Run run) {
		return new RunStatus(run);
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

			switch (run.getCategory()) {
			case Deployment:
				launchDeployment(run);
				break;
			case Image:
				launchImage(run);
				break;
			default:
				throw (new ServerExecutionEnginePluginException(
						"Cannot submit type: " + run.getCategory() + " yet!!"));
			}

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

	private RunStatus launchImage(Run run) {
		return null;
	}

	@Override
	public Credentials getCredentials(User user) {
		return new LocalCredentials(user);
	}

	@Override
	public void terminate(Run run, User user)
			throws SlipStreamException {
		String ids = run.getRuntimeParameterValue("orchestrator:hostname");

		if (ids == null) {
			return;
		}

		for (String nodeName : run.getNodeNames().split(",")) {
			nodeName = nodeName.trim();
			if("".equals(nodeName)) {
				continue;
			}
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

				ids += " " + run.getRuntimeParameterValue(ipKey);
			}
		}

		Logger.getLogger(this.getClass().getName()).info("Terminating: " + ids);

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
