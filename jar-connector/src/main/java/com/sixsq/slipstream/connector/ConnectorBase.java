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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.NotImplementedException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ExtraDisk;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.util.FileUtil;

public abstract class ConnectorBase implements Connector {

	abstract public String getCloudServiceName();

    abstract public Run launch(Run run, User user) throws SlipStreamException;

    abstract public Credentials getCredentials(User user);

    abstract public void terminate(Run run, User user) throws SlipStreamException;

    abstract public Properties describeInstances(User user) throws SlipStreamException;

    private static Logger log = Logger.getLogger(ConnectorBase.class.toString());

    protected static Logger getLog() {
        return log;
    }

    private static final String MACHINE_INSTANCE_ID_NAME = Run.MACHINE_NAME_PREFIX + RuntimeParameter.INSTANCE_ID_KEY;
    protected static final String MACHINE_INSTANCE_HOSTNAME = Run.MACHINE_NAME_PREFIX + RuntimeParameter.HOSTNAME_KEY;
    protected static final String MACHINE_INSTANCE_URL_SSH = Run.MACHINE_NAME_PREFIX + RuntimeParameter.URL_SSH_KEY;
    protected static final String SLIPSTREAM_REPORT_DIR = "/tmp/slipstream/reports";

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

    protected String getImageId(Run run, User user) throws SlipStreamClientException, ConfigurationException,
            ServerExecutionEnginePluginException {

        String imageId;

        if (isInOrchestrationContext(run)) {
            imageId = getOrchestratorImageId(user);
        } else {
            imageId = ((ImageModule) run.getModule()).extractBaseImageId(run.getCloudService());
        }
        return imageId;
    }

    protected String getOrchestratorImageId(User user) throws ValidationException,
            ServerExecutionEnginePluginException {
        return getCloudParameterValue(user, UserParametersFactoryBase.ORCHESTRATOR_IMAGEID_PARAMETER_NAME);
    }

    protected String getCloudParameterValue(User user, String paramName) throws ServerExecutionEnginePluginException,
            ValidationException {
        String qualifiedParamName = constructKey(paramName);
        String paramValue = user.getParameterValue(qualifiedParamName, null);
        if (paramValue == null) {
            throw (new ServerExecutionEnginePluginException("Missing parameter '" + qualifiedParamName + "'."));
        }
        return paramValue;
    }

    protected String getDefaultCloudServiceName(User user) throws ValidationException {
        return user.getDefaultCloudService();
    }

    public String getConnectorInstanceName() {
        return instanceName;
    }

    public void abort(Run run, User user) throws ServerExecutionEnginePluginException {
    }

    public void checkCredentials(Credentials credentials) throws InvalidElementException {
        if (credentials.getKey() == null) {
            throw (new InvalidElementException("Missing Cloud account key."));
        }
        if (credentials.getSecret() == null) {
            throw (new InvalidElementException("Missing Cloud account secret."));
        }
    }

    protected Run updateInstanceIdAndIpOnRun(Run run, String instanceId, String ipAddress) throws NotFoundException,
            ValidationException, ServerExecutionEnginePluginException {
        return updateInstanceIdAndIpOnRun(run, instanceId, ipAddress, getOrchestratorName(run));
    }

    protected Run updateInstanceIdAndIpOnRun(Run run, String instanceId, String ipAddress,
                                             String orchestratorName) throws NotFoundException, ValidationException,
            ServerExecutionEnginePluginException {

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

    protected String constructScriptObfuscateCommand(Run run, User user) throws IOException, SlipStreamClientException {
        String sshUsername = getLoginUsername(run);
        String command = "";
        // command +=
        // "sed -r -i 's/# *(account +required +pam_access\\.so).*/\\1/' /etc/pam.d/login\n";
        // command += "echo '-:ALL:LOCAL' >> /etc/security/access.conf\n";
        command += "sed -i '/RSAAuthentication/d' /etc/ssh/sshd_config\n";
        command += "sed -i '/PubkeyAuthentication/d' /etc/ssh/sshd_config\n";
        command += "sed -i '/PasswordAuthentication/d' /etc/ssh/sshd_config\n";
        command += "echo -e 'RSAAuthentication yes\nPubkeyAuthentication yes\nPasswordAuthentication no\n' >> " +
                "/etc/ssh/sshd_config\n";
        command += "umask 077\n";
        command += "mkdir -p ~/.ssh\n";
        command += "echo '" + getPublicSshKey(run, user) + "' >> ~/.ssh/authorized_keys\n";
        command += "chown -R " + sshUsername + ":$(id -g " + sshUsername + ")" + " ~/.ssh\n";
        // If SELinux is installed and enabled.
        command += "restorecon -Rv ~/.ssh || true\n";
        command += "[ -x /etc/init.d/sshd ] && { service sshd reload; } || { service ssh reload; }\n";
        return command;
    }

    protected String getPrivateSshKey() throws ConfigurationException, ValidationException {
        String privateSshKeyFile = getPrivateSshKeyFileName();
        return FileUtil.fileToString(privateSshKeyFile);
    }

    protected String getPrivateSshKeyFileName() throws ConfigurationException, ValidationException {
        String privateSshKeyFile = Configuration.getInstance()
                                                .getProperty("cloud.connector.orchestrator.privatesshkey");
        return privateSshKeyFile;
    }

    protected String getPublicSshKey(Run run, User user) throws ValidationException, IOException {
        String publicSshKeyFile = getPublicSshKeyFileName(run, user);
        return FileUtil.fileToString(publicSshKeyFile);
    }

    protected String getPublicSshKeyFileName(Run run, User user) throws IOException, ValidationException {
        String publicSshKey;
        if (run.getType() == RunType.Run) {
            tempSshKeyFile = File.createTempFile("sshkey", ".tmp");
            BufferedWriter out = new BufferedWriter(new FileWriter(tempSshKeyFile));
            String sshPublicKey = user.getParameter(
                    ExecutionControlUserParametersFactory.CATEGORY + "." + UserParametersFactoryBase
                            .SSHKEY_PARAMETER_NAME
            ).getValue();
            out.write(sshPublicKey);
            out.close();
            publicSshKey = tempSshKeyFile.getPath();
        } else {
            publicSshKey = Configuration.getInstance().getProperty(ServiceConfiguration.CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY);
        }
        return publicSshKey;
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

        for (String nodeName : run.getNodeNameList()) {
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

    public Map<String, ModuleParameter> getImageParametersTemplate() throws ValidationException {
        return new HashMap<String, ModuleParameter>();
    }

    protected String generateCookie(String identifier) {
        Properties extraProperties = new Properties();
        extraProperties.put(CookieUtils.COOKIE_IS_MACHINE, "true");
        return CookieUtils.createCookie(identifier, getConnectorInstanceName(), extraProperties);
    }

    protected String getCookieForEnvironmentVariable(String identifier) {
        return "\"" + generateCookie(identifier) + "\"";
    }

    protected abstract String constructKey(String key) throws ValidationException;

    protected String getVerboseParameterValue(User user) throws ValidationException {
        return user.getParameterValue(Parameter.constructKey(ExecutionControlUserParametersFactory.VERBOSITY_LEVEL),
                ExecutionControlUserParametersFactory.VERBOSITY_LEVEL_DEFAULT
        );
    }

    public String getOrchestratorName(Run run) {
        String orchestratorName = Run.ORCHESTRATOR_NAME;

        if (isInOrchestrationContext(run)) {
            orchestratorName = Run.constructOrchestratorName(getConnectorInstanceName());
        }

        return orchestratorName;
    }

    protected String getInstanceType() {
        // TODO Auto-generated method stub
        return null;
    }

    protected String getInstanceType(ImageModule image) throws ValidationException {
        return getParameterValue(ImageModule.INSTANCE_TYPE_KEY, image);
    }

    protected String getCpu(ImageModule image) throws ValidationException {
        return getParameterValue(ImageModule.CPU_KEY, image);
    }

    protected String getRam(ImageModule image) throws ValidationException {
        return getParameterValue(ImageModule.RAM_KEY, image);
    }

    protected String getParameterValue(String parameterName, ImageModule image) throws ValidationException {
        ModuleParameter instanceTypeParameter = image.getParameter(constructKey(parameterName));
        return instanceTypeParameter == null ? null : instanceTypeParameter.getValue();
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

    protected String getLoginUsername(Run run) throws SlipStreamClientException {
        if (isInOrchestrationContext(run)) {
            return getOrchestratorImageLoginUsername();
        } else {
            return getMachineImageLoginUsername(run);
        }
    }

    private String getOrchestratorImageLoginUsername() throws ConfigurationException, ValidationException {
        return Configuration.getInstance().getRequiredProperty(constructKey("orchestrator.ssh.username"));
    }

    private String getMachineImageLoginUsername(Run run) throws SlipStreamClientException {

        ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
        String username = machine.getLoginUser();
        if (username == null) {
            throw (new SlipStreamClientException("Module " + machine.getName() + " is missing login username"));
        }
        return username;
    }

    protected String getLoginPassword(Run run) throws ConfigurationException, SlipStreamClientException {
        if (isInOrchestrationContext(run)) {
            return getOrchestratorImageLoginPassword();
        } else {
            return getMachineImageLoginPassword(run);
        }
    }

    private String getOrchestratorImageLoginPassword() throws ConfigurationException, ValidationException {
        return Configuration.getInstance().getRequiredProperty(constructKey("orchestrator.ssh.password"));
    }

    private String getMachineImageLoginPassword(Run run) throws SlipStreamClientException {

        ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
        String password = machine.getParameterValue(constructKey(ImageModule.LOGINPASSWORD_KEY), null);
        if (password == null) {
            throw (new SlipStreamClientException("Module " + machine.getName() + " is missing ssh login password"));
        }
        return password;
    }

    protected String getEndpoint(User user) {
        return user.getParameter(getConnectorInstanceName() + "." + UserParametersFactoryBase.ENDPOINT_PARAMETER_NAME
        ).getValue();
    }
}
