package com.sixsq.slipstream.connector;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2014 SixSq Sarl (sixsq.com)
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

import com.google.gson.JsonObject;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.*;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.run.RuntimeParameterMediator;
import com.sixsq.slipstream.util.FileUtil;
import com.sixsq.slipstream.util.ServiceOffersUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public abstract class ConnectorBase implements Connector {

    abstract public String getCloudServiceName();

    abstract public Run launch(Run run, User user) throws SlipStreamException;

    abstract public Credentials getCredentials(User user);

    abstract public void terminate(Run run, User user) throws SlipStreamException;

    abstract public Map<String, Properties> describeInstances(User user, int timeout) throws SlipStreamException;

    public static final String VM_STATE = "state";
    public static final String VM_IP = "ip";
    public static final String VM_CPU = "cpu";
    public static final String VM_RAM = "ram";
    public static final String VM_DISK = "disk";
    public static final String VM_INSTANCE_TYPE = "instance-type";

    private static Logger log = Logger.getLogger(ConnectorBase.class.toString());

    protected static Logger getLog() {
        return log;
    }

    private static final String MACHINE_INSTANCE_ID_NAME = Run.MACHINE_NAME_PREFIX + RuntimeParameter.INSTANCE_ID_KEY;
    protected static final String MACHINE_INSTANCE_HOSTNAME = Run.MACHINE_NAME_PREFIX + RuntimeParameter.HOSTNAME_KEY;
    protected static final String MACHINE_INSTANCE_URL_SSH = Run.MACHINE_NAME_PREFIX + RuntimeParameter.URL_SSH_KEY;

    // TODO: shouldn't be there!!
    private Map<String, Map<String, String>> extraDisksInfo = new HashMap<String, Map<String, String>>();

    private File tempSshKeyFile;

    private final String instanceName;

    /**
     * Is a deployment or a build image, where both cases require an orchestrator
     */
    public static boolean isInOrchestrationContext(Run run) {
        return run.getType() == RunType.Orchestration || run.getType() == RunType.Machine;
    }

    public ConnectorBase(String instanceName) {
        this.instanceName = instanceName;
    }

    protected static Credentials getCredentialsObject(User user) throws ConfigurationException, ValidationException {
        return ConnectorFactory.getCurrentConnector(user).getCredentials(user);
    }

    protected String getImageId(Run run, User user) throws ConfigurationException, ValidationException {
        String imageId;

        if (isInOrchestrationContext(run)) {
            imageId = getOrchestratorImageId(user);
        } else {
            String cloudService = run.getCloudServiceNameForNode(Run.MACHINE_NAME);
            imageId = ((ImageModule) run.getModule()).extractBaseImageId(cloudService);
        }
        return imageId;
    }

    protected String getOrchestratorImageId(User user) throws ValidationException {
        return getCloudParameterValue(user, UserParametersFactoryBase.ORCHESTRATOR_IMAGEID_PARAMETER_NAME);
    }

    protected String getCloudParameterValue(User user, String paramName)
            throws ConfigurationException, ValidationException {
        String qualifiedParamName = constructKey(paramName);
        String paramValue = user.getParameterValue(qualifiedParamName, null);
        if (paramValue == null) {
            throw (new ConfigurationException("Missing parameter '" + qualifiedParamName + "'."));
        }
        return paramValue;
    }

    protected String getCloudParameterValue(User user, String paramName, String defaultValue)
            throws ValidationException {
        String qualifiedParamName = constructKey(paramName);
        return user.getParameterValue(qualifiedParamName, defaultValue);
    }

    protected String getDefaultCloudServiceName(User user) throws ValidationException {
        return user.getDefaultCloudService();
    }

    public String getConnectorInstanceName() {
        return instanceName;
    }

    public void checkCredentials(Credentials credentials) throws InvalidElementException {
        if (credentials.getKey() == null) {
            throw (new InvalidElementException("Missing Cloud account key."));
        }
        if (credentials.getSecret() == null) {
            throw (new InvalidElementException("Missing Cloud account secret."));
        }
    }

    protected Run updateInstanceIdAndIpOnRun(Run run, String instanceId, String ipAddress)
            throws NotFoundException, ValidationException, ServerExecutionEnginePluginException {
        return updateInstanceIdAndIpOnRun(run, instanceId, ipAddress, getOrchestratorName(run));
    }

    protected Run updateInstanceIdAndIpOnRun(Run run, String instanceId, String ipAddress, String orchestratorName)
            throws NotFoundException, ValidationException, ServerExecutionEnginePluginException {

        if (isInOrchestrationContext(run)) {
            updateOrchestratorInstanceIdOnRun(run, instanceId, orchestratorName);
            updateOrchestratorInstanceIpOnRun(run, ipAddress, orchestratorName);
            updateOrchestratorUrlSshOnRun(run, ipAddress, orchestratorName);
        } else {
            updateMachineInstanceIdOnRun(run, instanceId);
            updateMachineInstanceIpOnRun(run, ipAddress);
            updateMachineInstanceUrlSshOnRun(run, ipAddress);
        }
        return run;
    }

    private void updateOrchestratorInstanceIdOnRun(Run run, String instanceId, String orchestratorName) throws
            NotFoundException, ValidationException {

        String orchestratorInstanceIdName = orchestratorName + RuntimeParameter.NODE_PROPERTY_SEPARATOR +
                RuntimeParameter.INSTANCE_ID_KEY;

        setRuntimeParameterValue(orchestratorInstanceIdName, instanceId, run);
    }

    private void updateOrchestratorInstanceIpOnRun(Run run, String instanceHostname, String orchestratorName) throws
            NotFoundException, ValidationException {

        String machineInstanceHostname = orchestratorName + RuntimeParameter.NODE_PROPERTY_SEPARATOR +
                RuntimeParameter.HOSTNAME_KEY;

        setRuntimeParameterValue(machineInstanceHostname, instanceHostname, run);
    }

    private void updateOrchestratorUrlSshOnRun(Run run, String instanceHostname, String orchestratorName) throws
            NotFoundException, ValidationException {

        String machineInstanceUrlSshKey = orchestratorName
                .trim() + RuntimeParameter.NODE_PROPERTY_SEPARATOR + RuntimeParameter.URL_SSH_KEY;

        String url = getSshUrl(run, instanceHostname);
        setRuntimeParameterValue(machineInstanceUrlSshKey, url, run);
    }

    private void setRuntimeParameterValue(String key, String value, Run run) {
        RuntimeParameter p = RuntimeParameter.loadFromUuidAndKey(run.getUuid(), key);
        p.setValue(value);
        RuntimeParameterMediator.processSpecialValue(p);
        p.store();
    }

    private void updateMachineInstanceIdOnRun(Run run, String instanceId) throws NotFoundException,
            ValidationException {
        setRuntimeParameterValue(MACHINE_INSTANCE_ID_NAME, instanceId, run);
    }

    private void updateMachineInstanceIpOnRun(Run run, String instanceHostname) throws NotFoundException,
            ValidationException {
        setRuntimeParameterValue(MACHINE_INSTANCE_HOSTNAME, instanceHostname, run);
    }

    private void updateMachineInstanceUrlSshOnRun(Run run, String instanceHostname) throws NotFoundException,
            ValidationException {

        String url = getSshUrl(run, instanceHostname);
        setRuntimeParameterValue(MACHINE_INSTANCE_URL_SSH, url, run);
    }

    private String getSshUrl(Run run, String instanceHostname) {

        String user = null;
        try {
            user = getLoginUsername(run);
        } catch (SlipStreamClientException e) {
            user = "root";
        } catch (ConfigurationException e) {
            user = "root";
        }
        return String.format("ssh://%s@%s", user.trim(), instanceHostname.trim());
    }

    protected void defineExtraDisk(String name, String description, String regex, String regexError) {
        Map<String, String> diskInfo = new HashMap<String, String>();
        diskInfo.put(ExtraDisk.EXTRADISK_KEY_DESCRIPTION, description);
        diskInfo.put(ExtraDisk.EXTRADISK_KEY_REGEX, regex);
        diskInfo.put(ExtraDisk.EXTRADISK_KEY_REGEXERROR, regexError);

        extraDisksInfo.put(name, Collections.unmodifiableMap(diskInfo));
    }

    public List<ExtraDisk> getExtraDisks() {
        List<ExtraDisk> disks = new ArrayList<ExtraDisk>();

        for (Map.Entry<String, Map<String, String>> entry : extraDisksInfo.entrySet()) {
            String name = entry.getKey();
            Map<String, String> valuesList = entry.getValue();
            ExtraDisk disk = new ExtraDisk(name, valuesList.get(ExtraDisk.EXTRADISK_KEY_DESCRIPTION));
            disks.add(disk);
        }

        return disks;
    }

    public void validateExtraDiskParameter(String name, String param) throws ValidationException {
        if (param == null || param.isEmpty()) {
            return;
        }
        Map<String, String> diskInfo = extraDisksInfo.get(name);
        if (!param.matches(diskInfo.get(ExtraDisk.EXTRADISK_KEY_REGEX))) {
            throw (new ValidationException(diskInfo.get(ExtraDisk.EXTRADISK_KEY_REGEXERROR)));
        }
    }

    protected String getPrivateSshKey() throws ConfigurationException, ValidationException {
        String privateSshKeyFile = getPrivateSshKeyFileName();
        return FileUtil.fileToString(privateSshKeyFile);
    }

    protected String getPrivateSshKeyFileName() throws ConfigurationException, ValidationException {
        String privateSshKeyFile = Configuration.getInstance()
                .getProperty(ServiceConfiguration.CLOUD_CONNECTOR_ORCHESTRATOR_PRIVATESSHKEY);
        return privateSshKeyFile;
    }

    public static String getServerPublicSshKeyFilename() throws ConfigurationException, ValidationException {
        return Configuration.getInstance().getProperty(ServiceConfiguration.CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY);
    }

    private String getUserPublicSshKey(User user) throws ValidationException, IOException {
        return user.getParameter(Parameter.constructKey(
                ExecutionControlUserParametersFactory.CATEGORY, UserParametersFactoryBase.SSHKEY_PARAMETER_NAME))
                .getValue();
    }

    private String getUserOrServerPublicSshKey(User user) throws ValidationException, IOException {
        String userPublicSshKey = getUserPublicSshKey(user);
        if (userPublicSshKey == null || userPublicSshKey.trim().isEmpty()) {
            return FileUtil.fileToString(getServerPublicSshKeyFilename());
        } else {
            return userPublicSshKey;
        }
    }

    protected String getPublicSshKey(Run run, User user) throws ValidationException, IOException {
        if (run.getType() == RunType.Run) {
            return getUserOrServerPublicSshKey(user);
        } else {
            String publicSshKeyFile = getPublicSshKeyFileName(run, user);
            return FileUtil.fileToString(publicSshKeyFile);
        }
    }

    protected String getPublicSshKeyFileName(Run run, User user) throws IOException, ValidationException {
        String publicSshKeyFilename;
        if (run.getType() == RunType.Run) {
            tempSshKeyFile = File.createTempFile("sshkey", ".tmp");
            BufferedWriter out = new BufferedWriter(new FileWriter(tempSshKeyFile));
            String sshPublicKey = getUserOrServerPublicSshKey(user);
            out.write(sshPublicKey);
            out.close();
            publicSshKeyFilename = tempSshKeyFile.getPath();
        } else {
            publicSshKeyFilename = getServerPublicSshKeyFilename();
        }
        return publicSshKeyFilename;
    }

    protected void deleteTempSshKeyFile() {
        if (tempSshKeyFile != null) {
            if (!tempSshKeyFile.delete()) {
                getLog().warning("cannot delete SSH key file: " + tempSshKeyFile.getPath());
            }
        }
    }

    protected List<String> getCloudNodeInstanceIds(Run run) throws NotFoundException, ValidationException {
        List<String> ids = new ArrayList<String>();

        for (String nodeName : run.getNodeInstanceNamesList()) {
            nodeName = nodeName.trim();

            String idKey = nodeName + RuntimeParameter.NODE_PROPERTY_SEPARATOR + RuntimeParameter.INSTANCE_ID_KEY;

            String cloudServiceKey = nodeName + RuntimeParameter.NODE_PROPERTY_SEPARATOR + RuntimeParameter
                    .CLOUD_SERVICE_NAME;

            String id = run.getRuntimeParameterValueIgnoreAbort(idKey);
            String cloudService = run.getRuntimeParameterValueIgnoreAbort(cloudServiceKey);

            if (id != null && !id.equals("") && this.getConnectorInstanceName().equals(cloudService)) {
                ids.add(id);
            }
        }
        return ids;
    }

    public Map<String, UserParameter> getUserParametersTemplate() throws ValidationException {
        throw (new NotImplementedException());
    }

    public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate() throws
            ValidationException {
        throw (new NotImplementedException());
    }

    /**
     * Implement in connector class if extra parameters are required to be set during
     * XML serialization of the User object when UserResource is called to get XML.
     */
    public void setExtraUserParameters(User user) throws ValidationException {
    }

    public Map<String, ModuleParameter> getImageParametersTemplate() throws ValidationException {
        return new HashMap<String, ModuleParameter>();
    }

    protected String generateCookie(String identifier, String runId) {
        Properties extraProperties = new Properties();
        extraProperties.put(CookieUtils.COOKIE_IS_MACHINE, "true");
        extraProperties.put(CookieUtils.COOKIE_RUN_ID, runId);
        extraProperties.put(CookieUtils.COOKIE_EXPIRY_DATE, "0");

        String cookie = CookieUtils.createCookie(identifier, getConnectorInstanceName(), extraProperties);

        getLog().info("Generated cookie = " + cookie);

        return cookie;
    }

    protected String getCookieForEnvironmentVariable(String identifier, String runId) {
        return "\"" + generateCookie(identifier, runId) + "\"";
    }

    protected abstract String constructKey(String key) throws ValidationException;

    protected String getVerboseParameterValue(User user) {
        return user.getParameterValue(Parameter.constructKey(ExecutionControlUserParametersFactory.VERBOSITY_LEVEL),
                ExecutionControlUserParametersFactory.VERBOSITY_LEVEL_DEFAULT
        );
    }

    protected String getInstanceName(Run run) {
        return (isInOrchestrationContext(run)) ? getOrchestratorName(run) : Run.MACHINE_NAME;
    }

    public String getOrchestratorName(Run run) {
        String orchestratorName = Run.ORCHESTRATOR_NAME;

        if (isInOrchestrationContext(run)) {
            orchestratorName = Run.constructOrchestratorName(getConnectorInstanceName());
        }

        return orchestratorName;
    }

    protected String getInstanceType() {
        return null;
    }

    protected String getInstanceType(Run run) throws ValidationException {
        return getParameterValue(ImageModule.INSTANCE_TYPE_KEY, run);
    }

    protected String getInstanceType(ImageModule image) throws ValidationException {
        return getParameterValue(ImageModule.INSTANCE_TYPE_KEY, image);
    }

    protected String getCpu(Run run) throws ValidationException {
        return getParameterValue(ImageModule.CPU_KEY, run);
    }

    protected String getCpu(ImageModule image) throws ValidationException {
        return getParameterValue(ImageModule.CPU_KEY, image);
    }

    protected String getRam(Run run) throws ValidationException {
        return getParameterValue(ImageModule.RAM_KEY, run);
    }

    protected String getRam(ImageModule image) throws ValidationException {
        return getParameterValue(ImageModule.RAM_KEY, image);
    }

    protected String getRootDisk(Run run) throws ValidationException {
        return getParameterValue(ImageModule.DISK_PARAM, run);
    }

    protected String getRootDisk(ImageModule image) throws ValidationException {
        return getParameterValue(ImageModule.DISK_PARAM, image);
    }

    protected String getExtraDiskVolatile(Run run) throws ValidationException {
        return getParameterValue(ImageModule.EXTRADISK_VOLATILE_PARAM, run);
    }

    protected String getExtraDiskVolatile(ImageModule image) throws ValidationException {
        return getParameterValue(ImageModule.EXTRADISK_VOLATILE_PARAM, image);
    }

    protected String getParameterValue(String parameterName, Run run) throws ValidationException {
        return getParameterValue(parameterName, run, null);
    }

    protected String getParameterValue(String parameterName, ImageModule image) throws ValidationException {
        return getParameterValue(parameterName, null, image);
    }

    protected String getParameterValue(String parameterName, Run run, ImageModule image) throws ValidationException {
        if (run != null) {
            if (run.getCategory() != ModuleCategory.Image) {
                throw new RuntimeException("getParameterValue method can only be used with a " +
                        "ModuleCategory.Image (component). Please contact your SlipStream administrator");
            }

            try {
                String baseName = Parameter.constructKey(this.getConnectorInstanceName(), parameterName);
                String fullName = RuntimeParameter.constructParamName(Run.MACHINE_NAME, baseName);
                return run.getRuntimeParameterValue(fullName);
            } catch (Exception e1) {}

            try {
                String fullName = RuntimeParameter.constructParamName(Run.MACHINE_NAME, parameterName);
                return run.getRuntimeParameterValue(fullName);
            } catch (Exception e2) {}

            if (image == null) {
                image = (ImageModule) run.getModule();
            }
        }

        if (image != null) {
            return image.getParameterValue(constructKey(parameterName), null);
        }

        return null;
    }

    protected String getKey(User user) {
        try {
            return getCredentials(user).getKey();
        } catch (InvalidElementException e) {
            return null;
        } catch (ConfigurationException e) {
            return null;
        }
    }

    protected String getSecret(User user) {
        try {
            return getCredentials(user).getSecret();
        } catch (InvalidElementException e) {
            return null;
        } catch (ConfigurationException e) {
            return null;
        }
    }

    @Override
    public boolean isCredentialsSet(User user) {
        String key = getKey(user);
        String secret = getSecret(user);

        return !(key == null || "".equals(key) || secret == null || "".equals(secret));
    }

    protected String getLoginUsername(Run run) throws ConfigurationException, ValidationException {
        if (isInOrchestrationContext(run)) {
            return getOrchestratorImageLoginUsername();
        } else {
            return getMachineImageLoginUsername(run);
        }
    }

    private String getOrchestratorImageLoginUsername() throws ConfigurationException, ValidationException {
        String key = constructKey(SystemConfigurationParametersFactoryBase.ORCHESTRATOR_USERNAME_KEY);
        return Configuration.getInstance().getRequiredProperty(key);
    }

    private String getMachineImageLoginUsername(Run run) throws ValidationException {
        ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
        String username = machine.getLoginUser();
        if (username == null) {
            throw new ValidationException("Module " + machine.getName() + " is missing login username");
        }
        return username;
    }

    protected String getLoginPassword(Run run) throws ConfigurationException, ValidationException {
        if (isInOrchestrationContext(run)) {
            return getOrchestratorImageLoginPassword();
        } else {
            return getMachineImageLoginPassword(run);
        }
    }

    private String getOrchestratorImageLoginPassword() throws ConfigurationException, ValidationException {
        String key = constructKey(SystemConfigurationParametersFactoryBase.ORCHESTRATOR_PASSWORD_KEY);
        return Configuration.getInstance().getRequiredProperty(key);
    }

    private String getMachineImageLoginPassword(Run run) throws ValidationException {

        ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
        String password = machine.getParameterValue(constructKey(ImageModule.LOGINPASSWORD_KEY), null);
        if (password == null) {
            throw new ValidationException("Module " + machine.getName() + " is missing ssh login password");
        }
        return password;
    }

    protected String getEndpoint(User user) throws ValidationException {
        String paramName = getConnectorInstanceName() + "." + UserParametersFactoryBase.ENDPOINT_PARAMETER_NAME;
        UserParameter endpointParam = user.getParameter(paramName);
        if (endpointParam != null) {
            return endpointParam.getValue();
        }
        throw new ValidationException("Failed to get endpoint. Parameter not found: " + paramName);
    }

    @Override
    public boolean isVmUsable(String vmState) {
        return "running".equalsIgnoreCase(vmState)
                || "active".equalsIgnoreCase(vmState)
                || "on".equalsIgnoreCase(vmState);
    }

    protected String constructCloudParameterName(String parameterName) {
        return Parameter.constructKey(getConnectorInstanceName(), parameterName);
    }

    protected static String getAttributeValueFromServiceOffer(JsonObject serviceOffer, String serviceOfferAttributeName,
                                                     String nodeInstanceName) throws ValidationException
    {
        try {
            return ServiceOffersUtil.getServiceOfferAttribute(serviceOffer, serviceOfferAttributeName);
        } catch (ValidationException e) {
            throw new ValidationException(e.getMessage() + " for the node instance '" + nodeInstanceName + "'");
        }
    }

    protected void setRuntimeParameterValueWithCheck(Run run, String parameterName, String nodeInstanceName,
                                                     String value) throws ValidationException{
        String runtimeParameterName = RuntimeParameter.constructParamName(nodeInstanceName, parameterName);
        RuntimeParameter runtimeParameter = run.getRuntimeParameters().get(runtimeParameterName);
        if (runtimeParameter == null) {
            throw new ValidationException("Failed to find the runtime parameter '" + runtimeParameterName + "' ");
        }
        runtimeParameter.setValue(value);
    }

    protected void setRuntimeParameterValueFromServiceOffer(Run run, JsonObject serviceOffer, String nodeInstanceName,
                                                            String parameterName, String serviceOfferAttributeName)
            throws ValidationException
    {
        String serviceOfferAttributeValue = getAttributeValueFromServiceOffer(serviceOffer,
                serviceOfferAttributeName, nodeInstanceName);
        setRuntimeParameterValueWithCheck(run, parameterName, nodeInstanceName, serviceOfferAttributeValue);
    }

    @Override
    public void applyServiceOffer(Run run, String nodeInstanceName, JsonObject serviceOffer)
            throws ValidationException {

    }

}
