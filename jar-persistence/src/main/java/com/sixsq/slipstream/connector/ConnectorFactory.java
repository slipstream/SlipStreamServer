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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration.RequiredParameters;
import com.sixsq.slipstream.persistence.User;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectorFactory {

    private static final String CONNECTOR_CLASS_SEPARATOR = ",";
	private static Map<String, Connector> connectors = null;

    public static Connector getCurrentConnector(User user) throws ConfigurationException, ValidationException {
        String cloudServiceName = getDefaultCloudServiceName(user);

        if ("".equals(cloudServiceName)) {
            // user not configured, we take the first connector
            cloudServiceName = getConnectors().entrySet().iterator().next().getKey();
        }

        if (!Parameter.hasValueSet(cloudServiceName)) {
            throwIncompleteUserCloudConfiguration(user);
        }

        return getConnector(cloudServiceName);
    }

    protected static void throwIncompleteUserCloudConfiguration(User user) throws ValidationException {
        throw new ValidationException(incompleteCloudConfigurationErrorMessage(user));
    }

    public static String incompleteCloudConfigurationErrorMessage(User user) {
        return "Incomplete cloud configuration. Consider editing your <a href='" + "/user/" + user
                .getName() + "?edit=true'>user account</a>";
    }

    public static Connector getConnector(String cloudServiceName) throws ConfigurationException, ValidationException {
        Connector connector = getConnectors().get(cloudServiceName);
        if (connector == null) {
            throw new ValidationException("Failed to load cloud connector: " + cloudServiceName);
        }
        return connector.copy();
    }

    public static Connector getConnector(String cloudServiceName, User user) throws ConfigurationException,
            ValidationException {
        if (CloudService.isDefaultCloudService(cloudServiceName)) {
            cloudServiceName = getDefaultCloudServiceName(user);
            if ("".equals(cloudServiceName)) {
                throw new ValidationException("Missing default cloud in user");
            }
        }

        return getConnector(cloudServiceName);
    }

    public static Connector instantiateConnectorFromName(String connectorClassName) throws InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        return (Connector) Class.forName(connectorClassName).getConstructor().newInstance();
    }

    public static String getDefaultCloudServiceName(User user) {
        return user.getDefaultCloudService();
    }

    @SuppressWarnings("unused")
    public static Connector loadConnector(String cloudServiceName) throws ConfigurationException {
        return loadConnector(cloudServiceName, null);
    }

    public static Connector loadConnector(String cloudServiceName, String instanceName) throws ConfigurationException {
        try {

            DiscoverableConnectorService svc = DiscoverableConnectorServiceLoader.getConnectorService(cloudServiceName);
            if (svc != null) {
                return svc.getInstance(instanceName);
            } else {
                throw new SlipStreamRuntimeException(
                        "cannot load cloud connector for " + cloudServiceName + " using key " +
                                convertClassNameToServiceName(cloudServiceName)
                );
            }

        } catch (Exception e) {
            throw new SlipStreamRuntimeException(e.getClass().getName() + " " + e.getMessage());
        }
    }

    public static void resetConnectors() {
        connectors = null;
    }

    public static void setConnectors(Map<String, Connector> inputConnectors) {
        Map<String, Connector> connectors = new HashMap<String, Connector>();
        connectors.putAll(inputConnectors);
        ConnectorFactory.connectors = Collections.unmodifiableMap(connectors);
    }

    public static Map<String, Connector> getConnectors(String[] classNames) throws ConfigurationException {
        if (connectors == null) {
            initializeConnectors(classNames);
        }
        return connectors;
    }

    public static void initializeConnectors(String[] classNames) throws ConfigurationException {

        Map<String, Connector> connectors = new HashMap<String, Connector>();

        Map<String, String> instanceNames = processConnectorInstanceConfig(classNames);
        for (Map.Entry<String, String> entry : instanceNames.entrySet()) {
            String instanceName = entry.getKey();
            String cloudServiceName = entry.getValue();

            Connector connector = loadConnector(cloudServiceName, instanceName);
            connectors.put(instanceName, connector);
        }

        setConnectors(connectors);
    }

    private static Map<String, String> processConnectorInstanceConfig(String[] instances) {

        Map<String, String> instanceMap = new HashMap<String, String>();

        for (String c : instances) {

            // A connector ID (either cloud service name or class name) with an optional instance name.
            String[] namePair = c.split(":");

            boolean isNamed = namePair.length > 1;

            // The loader will maintain compatibility for configurations with raw class names
            // rather than just the service names.
            String cloudServiceName = (isNamed) ? namePair[1].trim() : namePair[0].trim();

            // Default to cloud service name if no instance name is given.
            String instanceName = (isNamed) ? namePair[0].trim() : convertClassNameToServiceName(cloudServiceName);

            instanceMap.put(instanceName, cloudServiceName);
        }
        return instanceMap;
    }

    // If the argument looks to be a class name, then derive the cloud service name from the class name.  The cloud
    // service name will be the penultimate value when split on periods.  If there are no periods in the value,
    // then just return the value itself.
    public static String convertClassNameToServiceName(String configConnectorName) {
        String[] elements = configConnectorName.split("\\.");
        if (elements.length > 1) {
            String name = elements[elements.length - 2].toLowerCase();

            // Special case for the EC2 connector which doesn't follow the usual naming scheme of
            // residing is a directory named after the cloud service name.  (Directory is 'aws' but
            // the cloud service name is 'ec2'.)
            if ("aws".equals(name)) {
                return "ec2";
            } else {
                return name;
            }
        } else {
            return configConnectorName.toLowerCase();
        }
    }

    public static Map<String, Connector> getConnectors() throws ConfigurationException, ValidationException {
        return getConnectors(getConnectorClassNames());
    }

    // FIXME: hack to allow loading of configuration fail due to unavailability of db-serializers on
    // classpath.  This only possible during tests.
    private static String getConnectorClassNamesString() throws ConfigurationException, ValidationException {
        try {
            return Configuration.getInstance().getRequiredProperty(
                    RequiredParameters.CLOUD_CONNECTOR_CLASS.getName());
        } catch (ConfigurationException | ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            if (FileNotFoundException.class.isInstance(ex) && ex.getMessage().contains("db/serializers")) {
                return "";
            } else {
                throw ex;
            }
        }
    }

    public static String[] getConnectorClassNames() throws ConfigurationException, ValidationException {
        return splitConnectorClassNames(getConnectorClassNamesString());
    }

    public static String[] splitConnectorClassNames(String connectorsClassNames) {
        if (connectorsClassNames == null || connectorsClassNames.trim().isEmpty()) {
            return new String[0];
        }
        return connectorsClassNames.split(CONNECTOR_CLASS_SEPARATOR);
    }

    public static String assembleConnectorClassString(String[] connectorsClassNames) {
    	StringBuilder sb = new StringBuilder();
    	for(String c : connectorsClassNames) {
    		sb.append(c);
    		sb.append(CONNECTOR_CLASS_SEPARATOR);
    	}
        return sb.toString();
    }

    public static List<String> getCloudServiceNamesList() throws ConfigurationException, ValidationException {
        return new ArrayList<String>(getConnectors().keySet());
    }

    public static String[] getCloudServiceNames() throws ConfigurationException, ValidationException {
        List<String> names = getCloudServiceNamesList();
        return names.toArray(new String[names.size()]);
    }

    public static String cloudNameFromInstanceName(final String connInstName) {
        if (connInstName.trim().isEmpty()) {
            return null;
        }
        Map<String, String> connInstNameToCloudMap = new HashMap<>();
        try {
            for (String c : ConnectorFactory.getConnectorClassNames()) {
                String instName;
                String cloudName;
                if (c.contains(":")) {
                    String[] parts = c.split(":");
                    instName = parts[0].trim();
                    cloudName = parts[1].trim();
                } else {
                    instName = c.trim();
                    cloudName = instName;
                }
                connInstNameToCloudMap.put(instName, cloudName);
            }
        } catch (ValidationException e) {
            return null;
        }
        return connInstNameToCloudMap.get(connInstName.trim());
    }
}
