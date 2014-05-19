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
import com.sixsq.slipstream.persistence.ServiceCatalog;
import com.sixsq.slipstream.persistence.ServiceCatalogs;
import com.sixsq.slipstream.persistence.ServiceConfiguration.RequiredParameters;
import com.sixsq.slipstream.persistence.User;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConnectorFactory {

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

    private static Connector loadConnector(String cloudServiceName) throws ConfigurationException {
        return loadConnector(cloudServiceName, null);
    }

    private static Connector loadConnector(String cloudServiceName, String instanceName) throws ConfigurationException {
        try {

            DiscoveryConnectorService stub = DiscoveryConnectorServiceLoader.getConnectorStub(cloudServiceName);
            if (stub != null) {
                return stub.getInstance(instanceName);
            } else {
                throw new SlipStreamRuntimeException("cannot load cloud connector for " + cloudServiceName);
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

            Connector connector = loadConnector(cloudServiceName);
            connectors.put(instanceName, connector);
        }

        setConnectors(connectors);

        updateServiceCatalog(connectors.keySet());
    }

    private static Map<String, String> processConnectorInstanceConfig(String[] instances) {

        Map<String, String> instanceMap = new HashMap<String, String>();

        for (String c : instances) {
            String[] nameAndClassName = c.split(":");

            boolean isNamed = nameAndClassName.length > 1;

            // For compatibility with old configurations, convert the class name to a service name.
            String cloudServiceName = (isNamed) ? nameAndClassName[1].trim() : nameAndClassName[0].trim();
            cloudServiceName = convertClassNameToServiceName(cloudServiceName);

            // Default to cloud service name for instance name.
            String instanceName = (isNamed) ? nameAndClassName[0].trim() : cloudServiceName;

            instanceMap.put(instanceName, cloudServiceName);
        }
        return instanceMap;
    }

    // If the argument looks to be a class name, then derive the cloud service name from the class name.  The cloud
    // service name will be the penultimate value when split on periods.  If there are no periods in the value,
    // then just return the value itself.
    private static String convertClassNameToServiceName(String className) {
        String[] elements = className.split(".");
        if (elements.length > 1) {
            return elements[elements.length - 2];
        } else {
            return className;
        }
    }

    private static void updateServiceCatalog(Set<String> cloudServiceNames) {

        ServiceCatalogs scs = new ServiceCatalogs();
        for (ServiceCatalog sc : scs.getList()) {
            if (!cloudServiceNames.contains(sc.getCloud())) {
                sc.remove();
            }
        }
        for (String cloud : cloudServiceNames) {
            boolean foundIt = false;
            for (ServiceCatalog sc : scs.getList()) {
                if (sc.getCloud().equals(cloud)) {
                    foundIt = true;
                    break;
                }
            }
            if (!foundIt) {
                ServiceCatalog sc = new ServiceCatalog(cloud);
                sc = sc.store();
                scs.getList().add(sc);
            }
        }
    }

    public static Map<String, Connector> getConnectors() throws ConfigurationException, ValidationException {
        return getConnectors(getConnectorClassNames());
    }

    public static String[] getConnectorClassNames() throws ConfigurationException, ValidationException {
        String connectorsClassNames = Configuration.getInstance().getRequiredProperty(
                RequiredParameters.CLOUD_CONNECTOR_CLASS.getName());

        return splitConnectorClassNames(connectorsClassNames);
    }

    public static String[] splitConnectorClassNames(String connectorsClassNames) {
        if (connectorsClassNames == null || connectorsClassNames.trim().isEmpty()) {
            return new String[0];
        }
        return connectorsClassNames.split(",");
    }

    public static List<String> getCloudServiceNamesList() throws ConfigurationException, ValidationException {
        return new ArrayList<String>(getConnectors().keySet());
    }

    public static String[] getCloudServiceNames() throws ConfigurationException, ValidationException {
        List<String> names = getCloudServiceNamesList();
        return names.toArray(new String[names.size()]);
    }

}
