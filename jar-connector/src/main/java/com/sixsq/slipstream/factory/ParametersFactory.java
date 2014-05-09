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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.ExecutionControlUserParametersFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ExtraDisk;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

public class ParametersFactory {

	public static List<ExtraDisk> getExtraDisks(
			Map<String, Connector> connectors) {
		List<ExtraDisk> disks = new ArrayList<ExtraDisk>();
		for (Connector c : connectors.values()) {
			disks.addAll(c.getExtraDisks());
		}
		return disks;
	}

	private static Map<String, ModuleParameter> getImageParametersTemplate(
			Map<String, Connector> connectors) throws ValidationException {
		Map<String, ModuleParameter> parameters = new HashMap<String, ModuleParameter>();
		for (Connector c : connectors.values()) {
			parameters.putAll(c.getImageParametersTemplate());
		}
		return parameters;
	}

	public static Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate(
			String[] connectorClassNames) throws ValidationException {
		Map<String, Connector> connectors = ConnectorFactory
				.getConnectors(connectorClassNames);
		return getServiceConfigurationParametersTemplate(connectors);
	}

	private static Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate(
			Map<String, Connector> connectors) throws ValidationException {

		Map<String, ServiceConfigurationParameter> parameters = new HashMap<String, ServiceConfigurationParameter>();
		for (Connector c : connectors.values()) {
			parameters.putAll(c.getServiceConfigurationParametersTemplate());
		}
		return parameters;
	}

	public static User addParametersForEditing(User user)
			throws ValidationException, ConfigurationException {

		Map<String, UserParameter> execParameters = new ExecutionControlUserParametersFactory()
				.getParameters();

		Map<String, Connector> connectors = ConnectorFactory.getConnectors();

		Map<String, UserParameter> templateParameters = ParametersFactory
				.getUserParametersTemplate(connectors);

		templateParameters.putAll(execParameters);

		for (Entry<String, UserParameter> template : templateParameters
				.entrySet()) {
			UserParameter templateParam = template.getValue();
			UserParameter existingParam = user.getParameter(templateParam
					.getName());
			if (existingParam != null) {
				templateParam.setValue(existingParam.getValue());
			}
			user.setParameter(templateParam);
		}

		resetCloudServiceNameEnum(user, execParameters);

		return user;
	}

	protected static void resetCloudServiceNameEnum(User user,
			Map<String, UserParameter> execParameters) {
		// Reset enum for cloud service name (in case connectors changed since)
		String cloudServiceNameKey = Parameter
				.constructKey(
						ParameterCategory.General.toString(),
						ExecutionControlUserParametersFactory.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME);
		UserParameter cloudServiceParameter = user
				.getParameter(cloudServiceNameKey);
		cloudServiceParameter.setEnumValues(execParameters.get(
				cloudServiceNameKey).getEnumValues());
	}

	private static Map<String, UserParameter> getUserParametersTemplate(
			Map<String, Connector> connectors) throws ValidationException {
		Map<String, UserParameter> parameters = new HashMap<String, UserParameter>();
		for (Connector c : connectors.values()) {
			parameters.putAll(c.getUserParametersTemplate());
		}
		return parameters;
	}

	public static Module addParametersForEditing(Module module)
			throws ValidationException, ConfigurationException {

		Map<String, Connector> connectors = ConnectorFactory.getConnectors();
		Map<String, ModuleParameter> templateParameters = new HashMap<String, ModuleParameter>();
		if (module.getCategory() == ModuleCategory.Image) {
			templateParameters = ParametersFactory
					.getImageParametersTemplate(connectors);
		}

		setParameters(module, templateParameters);

		return module;
	}

	private static void setParameters(Module module,
			Map<String, ModuleParameter> parameters) throws ValidationException {
		for (Entry<String, ModuleParameter> entry : parameters.entrySet()) {
			if (!module.parametersContainKey(entry.getKey())) {
				module.setParameter(entry.getValue());
			}
		}
	}

}
