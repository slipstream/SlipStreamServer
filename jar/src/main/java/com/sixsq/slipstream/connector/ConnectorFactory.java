package com.sixsq.slipstream.connector;

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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.ServiceConfiguration.RequiredParameters;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.user.FormProcessor;
import com.sun.xml.internal.fastinfoset.util.StringArray;

public class ConnectorFactory {

	private static Map<String, Connector> connectors = null;
	private static List<String> cloudServiceNames = null;

	public static Connector getCurrentConnector(User user)
			throws ConfigurationException, ValidationException {
		String cloudServiceName = getDefaultCloudServiceName(user);

		if ("".equals(cloudServiceName)) {
			// user not configured, we take the first connector
			cloudServiceName = getConnectors().entrySet().iterator().next()
					.getKey();
		}

		if (!FormProcessor.isSet(cloudServiceName)) {
			throwIncompleteUserCloudConfiguration(user);
		}

		return getConnector(cloudServiceName);
	}

	protected static void throwIncompleteUserCloudConfiguration(User user)
			throws ValidationException {
		throw new ValidationException(
				incompleteCloudConfigurationErrorMessage(user));
	}

	public static String incompleteCloudConfigurationErrorMessage(User user) {
		return "Incomplete cloud configuration. Consider editing your <a href='"
				+ "/user/" + user.getName()
				+ "?edit=true'>user account</a>";
	}

	public static Connector getConnector(String cloudServiceName)
			throws ConfigurationException, ValidationException {
		Connector connector = getConnectors().get(cloudServiceName);
		if (connector == null) {
			throw (new ValidationException("Failed to load cloud connector: "
					+ cloudServiceName));
		}
		return connector.copy();
	}

	public static Connector getConnector(String cloudServiceName, User user)
			throws ConfigurationException, ValidationException {
		if (isDefaultCloudService(cloudServiceName)) {
			cloudServiceName = getDefaultCloudServiceName(user);
			if ("".equals(cloudServiceName)) {
				throw new ValidationException("Missing default cloud in user");
			}
		}

		return getConnector(cloudServiceName);
	}

	public static boolean isDefaultCloudService(String cloudServiceName) {
		return "".equals(cloudServiceName)
				|| CloudImageIdentifier.DEFAULT_CLOUD_SERVICE
						.equals(cloudServiceName) || cloudServiceName == null;
	}

	public static Connector instantiateConnectorFromName(
			String connectorClassName) throws InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {
		return (Connector) Class.forName(connectorClassName).getConstructor()
				.newInstance();
	}

	public static String getDefaultCloudServiceName(User user) {
		return user.getDefaultCloudService();
	}

	private static Connector loadConnector(String className)
			throws ConfigurationException {
		return loadConnector(className, null);
	}

	private static Connector loadConnector(String className, String instanceName)
			throws ConfigurationException {
		try {
			if (instanceName == null) {
				return (Connector) Class.forName(className.trim())
						.getConstructor().newInstance();
			} else {
				return (Connector) Class.forName(className.trim())
						.getConstructor(String.class).newInstance(instanceName);
			}
		} catch (Exception e) {
			throw new SlipStreamRuntimeException(e.getClass().toString() + " "
					+ e.getMessage());
		}
	}

	public static void resetConnectors() {
		connectors = null;
		cloudServiceNames = null;
	}

	public static void setConnectors(Map<String, Connector> connectors) {
		ConnectorFactory.connectors = connectors;

		cloudServiceNames = new ArrayList<String>();

		for (Connector connector : connectors.values()) {
			setServiceName(connector);
		}
	}

	protected static void setServiceName(Connector connector) {
		cloudServiceNames.add(connector.getConnectorInstanceName());
	}

	public static Map<String, Connector> getConnectors(String[] classeNames)
			throws ConfigurationException {

		if (connectors != null) {
			return connectors;
		}

		connectors = new HashMap<String, Connector>();

		cloudServiceNames = new ArrayList<String>();
		for (String c : classeNames) {
			String[] nameAndClassName = c.split(":");

			boolean isNamed = nameAndClassName.length > 1;
			String className = (isNamed) ? nameAndClassName[1]
					: nameAndClassName[0];

			String name = "";
			Connector connector;
			if (isNamed) {
				name = nameAndClassName[0].trim();
				connector = loadConnector(className, name);
			} else {
				connector = loadConnector(className);
				name = connector.getConnectorInstanceName();
			}

			connectors.put(name, connector);
			setServiceName(connector);
		}

		return connectors;
	}

	public static Map<String, Connector> getConnectors()
			throws ConfigurationException {
		return getConnectors(getConnectorClassNames());
	}

	public static String[] getConnectorClassNames() {
		String connectorsClassNames = Configuration.getInstance()
				.getRequiredProperty(
						RequiredParameters.CLOUD_CONNECTOR_CLASS.getName());

		return splitConnectorClassNames(connectorsClassNames);
	}

	public static String[] splitConnectorClassNames(String connectorsClassNames) {
		if(connectorsClassNames.trim().isEmpty()) {
			return new String[0];
		}
		return connectorsClassNames.split(",");
	}

	public static List<String> getCloudServiceNamesList() {
		if (cloudServiceNames == null) {
			// will also set the cloud service names
			getConnectors();
		}
		return new ArrayList<String>(cloudServiceNames);
	}

	public static String[] getCloudServiceNames() {
		return getCloudServiceNamesList().toArray(new String[0]);
	}
}