package com.sixsq.slipstream.connector.okeanos;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.openstack.OpenStackConnector;
import com.sixsq.slipstream.connector.openstack.OpenStackImageParametersFactory;
import com.sixsq.slipstream.connector.openstack.OpenStackUserParametersFactory;
import com.sixsq.slipstream.exceptions.*;
import com.sixsq.slipstream.persistence.*;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.Utils;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.ssh.SshClient;
import org.jclouds.sshj.config.SshjSshClientModule;

import java.util.*;

/**
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
public class OkeanosConnector extends OpenStackConnector {
    public static final String CLOUD_SERVICE_NAME = "okeanos";
    public static final String JCLOUDS_DRIVER_NAME = "openstack-nova";
    public static final String CLOUDCONNECTOR_PYTHON_MODULENAME = "slipstream.cloudconnectors.okeanos.OkeanosClientCloud";

    public OkeanosConnector() {
        this(OkeanosConnector.CLOUD_SERVICE_NAME);

    }

    public OkeanosConnector(String instanceName) {
        super(instanceName);
    }

    @Override
    public Connector copy() {
        return new OkeanosConnector(getConnectorInstanceName());
    }

    public String getCloudServiceName() {
        return OkeanosConnector.CLOUD_SERVICE_NAME;
    }

    public String getJcloudsDriverName() {
        return OkeanosConnector.JCLOUDS_DRIVER_NAME;
    }


    protected Iterable<com.google.inject.Module> getContextBuilderModules() {
        return ImmutableSet.<com.google.inject.Module>of(
            new SLF4JLoggingModule(),
            new SshjSshClientModule()
        );
    }

    protected ContextBuilder newContextBuilder() {
        return ContextBuilder.newBuilder(getJcloudsDriverName());
    }

    protected ContextBuilder updateContextBuilder(ContextBuilder contextBuilder, User user, Properties overrides) {
        overrides.put("jclouds.ssh.max-retries", "1");
        overrides.put("jclouds.compute.timeout.script-complete", 1000L/*ms*/ * 60 /*sec*/ * 20 /*min*/);
        overrides.put(Constants.PROPERTY_PRETTY_PRINT_PAYLOADS, "true");
        overrides.put(Constants.PROPERTY_CONNECTION_TIMEOUT, "0");
        overrides.put(Constants.PROPERTY_SO_TIMEOUT, "0");

        return contextBuilder
            .modules(getContextBuilderModules())
            .credentials(getKey(user), getSecret(user))
            .overrides(overrides);
    }

    protected void updateContextBuilderPropertiesOverrides(User user, Properties overrides) throws ValidationException {
        if (overrides == null) {
            throw new NullPointerException("overrides");
        }

        overrides.setProperty(Constants.PROPERTY_ENDPOINT, user.getParameterValue(constructKey(OpenStackUserParametersFactory.ENDPOINT_PARAMETER_NAME),""));
        overrides.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true");
        overrides.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
        overrides.setProperty(KeystoneProperties.CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);
        overrides.setProperty(KeystoneProperties.REQUIRES_TENANT, "true");
        overrides.setProperty(KeystoneProperties.TENANT_NAME, user.getParameterValue(constructKey(OpenStackUserParametersFactory.TENANT_NAME), ""));

        overrides.setProperty(Constants.PROPERTY_API_VERSION, "v2.0");
    }

    protected ComputeService getComputeService() {
        return computeServiceContext.getComputeService();
    }

    protected void closeContext() {
        super.closeContext();
        computeServiceContext.close();
    }

    protected String getIpAddress(NovaApi client, String region, String instanceId) {
        final FluentIterable<? extends Server> instances = client.getServerApiForZone(region).listInDetail().concat();
        for(final Server instance : instances) {
            if(instance.getId().equals(instanceId)) {
                final Multimap<String, Address> addresses = instance.getAddresses();

                if(instance.getId().equals(instanceId)) {
                    if(addresses.size() > 0) {
                        if(addresses.containsKey("public")) {
                            final String addr = addresses.get("public").iterator().next().getAddr();
                            return addr;
                        }
                        else if(addresses.containsKey("private")) {
                            final String addr = addresses.get("private").iterator().next().getAddr();
                            return addr;
                        }
                        else {
                            final String instanceNetworkID = addresses.keySet().iterator().next();
                            final Collection<Address> instanceAddresses = addresses.get(instanceNetworkID);
                            if(instanceAddresses.size() > 0) {
                                final String addr = instanceAddresses.iterator().next().getAddr();
                                return addr;
                            }
                        }
                    }

                    break;
                }
            }
        }
        return "";
    }

    @Override
    protected NovaApi getClient(User user, Properties overrides) throws InvalidElementException, ValidationException {
        if (overrides == null) {
            overrides = new Properties();
        }
        updateContextBuilderPropertiesOverrides(user, overrides);

        final ContextBuilder contextBuilder = updateContextBuilder(newContextBuilder(), user, overrides);
        this.computeServiceContext = contextBuilder.buildView(ComputeServiceContext.class);
        this.context = this.computeServiceContext.unwrap();
        return this.context.getApi();
    }

    protected String createContextualizationDataPlus(
        Run run,
        User user,
        Configuration configuration,
        String extraLines
    ) throws ConfigurationException, ServerExecutionEnginePluginException, SlipStreamClientException {

        String logfilename = "orchestrator.slipstream.log";
        String bootstrap = "/tmp/slipstream.bootstrap";

        String username = user.getName();
        String password = user.getPassword();
        if (password == null) {
            throw (new ServerExecutionEnginePluginException(
                "Missing password entry in user profile"));
        }

        String userData = "#!/bin/sh -e \n";

        userData += "# SlipStream contextualization script for ~Okeanos\n";
        {
            userData += "## ++ EXTRA LINES\n";
            if(!extraLines.endsWith("\n")) extraLines += "\n";
            userData += extraLines;
            userData += "## -- EXTRA LINES\n";
        }
        userData += "export SLIPSTREAM_CLOUD=\"" + getCloudServiceName() + "\"\n";
        userData += "export SLIPSTREAM_CONNECTOR_INSTANCE=\"" + getConnectorInstanceName() + "\"\n";
        userData += "export SLIPSTREAM_NODENAME=\"" + getOrchestratorName(run) + "\"\n";
        userData += "export SLIPSTREAM_DIID=\"" + run.getName() + "\"\n";
        userData += "export SLIPSTREAM_REPORT_DIR=\"" + SLIPSTREAM_REPORT_DIR + "\"\n";
        userData += "export SLIPSTREAM_SERVICEURL=\"" + configuration.baseUrl + "\"\n";
        userData += "export SLIPSTREAM_BUNDLE_URL=\"" + configuration.getRequiredProperty("slipstream.update.clienturl") + "\"\n";
        userData += "export SLIPSTREAM_BOOTSTRAP_BIN=\"" + configuration.getRequiredProperty("slipstream.update.clientbootstrapurl") + "\"\n";
        userData += "export LIBCLOUD_BUNDLE_URL=\"" + configuration.getRequiredProperty("cloud.connector.library.libcloud.url") + "\"\n";
        userData += "export CLOUDCONNECTOR_PYTHON_MODULENAME=\"" + OkeanosConnector.CLOUDCONNECTOR_PYTHON_MODULENAME + "\"\n";
        userData += "export OPENSTACK_SERVICE_TYPE=\"" + configuration.getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_TYPE_PARAMETER_NAME)) + "\"\n";
        userData += "export OPENSTACK_SERVICE_NAME=\"" + configuration.getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_NAME_PARAMETER_NAME)) + "\"\n";
        userData += "export OPENSTACK_SERVICE_REGION=\"" + configuration.getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_REGION_PARAMETER_NAME)) + "\"\n";
        userData += "export SLIPSTREAM_CATEGORY=\"" + run.getCategory().toString() + "\"\n";
        userData += "export SLIPSTREAM_USERNAME=\"" + username + "\"\n";
        userData += "export SLIPSTREAM_COOKIE=" + getCookieForEnvironmentVariable(username) + "\n";
        userData += "export SLIPSTREAM_VERBOSITY_LEVEL=\"" + getVerboseParameterValue(user) + "\"\n";

        userData += "mkdir -p " + SLIPSTREAM_REPORT_DIR + "\n"
            + "wget --no-check-certificate -O " + bootstrap
            + " $SLIPSTREAM_BOOTSTRAP_BIN > " + SLIPSTREAM_REPORT_DIR + "/"
            + logfilename + " 2>&1 " + "&& chmod 0755 " + bootstrap + "\n"
            + bootstrap + " slipstream-orchestrator >> "
            + SLIPSTREAM_REPORT_DIR + "/" + logfilename + " 2>&1\n";

        System.out.print(userData);

        return userData;
    }

    // We use the same structure of the parent method and just change the details that are different
    // for Synnefo. In particular, we do not rely on the KeyPair OpenStack extension, but run scripts
    // via SSH directly.
    // NOTE From the currently provided Okeanos images, we cannot login to Ubuntu as root.
    //      This means that we must rely on a custom image, from which we can spawn Ubuntu servers
    //      for use by SlipStream.
    //      Using kamaki, after we create the custom image by just copying the official
    //      Ubuntu Server one, we enable root:
    //        $ kamaki image compute properties set CUSTOM_IMAGE_ID users="user root"
    @Override
    protected void launchDeployment(Run run, User user) throws ServerExecutionEnginePluginException, ClientExecutionEnginePluginException, InvalidElementException, ValidationException {
        System.out.println("launchDeployment()");
        Properties overrides = new Properties();
        NovaApi client = getClient(user, overrides);

        try {
            Configuration configuration = Configuration.getInstance();

            ImageModule imageModule = (run.getType() == RunType.Machine) ? ImageModule
                .load(run.getModuleResourceUrl()) : null;

            String region = configuration.getRequiredProperty(constructKey(OpenStackUserParametersFactory.SERVICE_REGION_PARAMETER_NAME));
            System.out.println("region = " + region);
            String imageId = (run.getType() == RunType.Orchestration)? getOrchestratorImageId(user) : getImageId(run, user);
            System.out.println("imageId = " + imageId);

            String instanceName = (run.getType() == RunType.Orchestration) ? getOrchestratorName(run) : imageModule.getShortName();
            System.out.println("instanceName = " + instanceName);

            String flavorName = (run.getType() == RunType.Orchestration) ? configuration
                .getRequiredProperty(constructKey(OpenStackUserParametersFactory.ORCHESTRATOR_INSTANCE_TYPE_PARAMETER_NAME))
                : getInstanceType(imageModule);
            System.out.println("flavorName = " + flavorName);
            String flavorId = getFlavorId(client, region, flavorName);
            System.out.println("flavorId = " + flavorId);
            String[] securityGroups = (run.getType() == RunType.Orchestration) ? "default".split(",")
                : getParameterValue(OpenStackImageParametersFactory.SECURITY_GROUPS, imageModule).split(",");
            System.out.println("securityGroups = " + Arrays.toString(securityGroups));

            String instanceData = "\n\nStarting instance on region '" + region + "'\n";
            instanceData += "Image id: " + imageId + "\n";
            instanceData += "Instance type: " + flavorName + "\n";
            System.out.println("instanceData = " + instanceData);

            CreateServerOptions options = CreateServerOptions.Builder
                .securityGroupNames(securityGroups);

            ServerCreated server = null;
            final ServerApi novaAPI = client.getServerApiForZone(region);
            try {
                server = novaAPI.create(instanceName, imageId, flavorId, options);
            }
            catch(Exception e) {
                e.printStackTrace();
                throw (new ServerExecutionEnginePluginException(e.getMessage()));
            }

            final String instanceId = server.getId();
            String ipAddress = "";

            while(ipAddress.isEmpty()) {
                try {
                    System.out.println("ipAddress is empty, sleeping...");
                    Thread.sleep(3000);
                }
                catch(InterruptedException ignored) {
                }
                ipAddress = getIpAddress(client, region, instanceId);
            }

            System.out.println("ipAddress = " + ipAddress);

            updateInstanceIdAndIpOnRun(run, instanceId, ipAddress);
            // force commit
            final String persistenceUnit = System.getProperty("persistence.unit","UNKNOWN");
            System.out.println("persistenceUnit = " + persistenceUnit);
            run.store();
//            try {
//                final EntityManager entityManager = PersistenceUtil.createEntityManager();
//                run = entityManager.merge(run);
//                final FlushModeType flashMode = entityManager.getFlushMode();
//                System.out.println("flashMode = " + flashMode);
//            }
//            catch(Exception e) {
//                e.printStackTrace();
//                System.out.println("Ignoring " + e);
//            }

            if(run.getType() != RunType.Orchestration) {
                return;
            }

            // Now run the initial script for the Orchestration VM
            final String nodeUsername = "root";
            System.out.println("nodeUsername = " + nodeUsername);
            final String nodePassword = server.getAdminPass();
            System.out.println("nodePassword = " + nodePassword);
            final String nodeId = String.format("%s/%s", region, instanceId);
            System.out.println("nodeId = " + nodeId);

            System.out.println("++==== INITIAL SCRIPT ========");
            final String extraLines = String.format(
                "export ORCHESTRATOR_VM_ROOT_PASSWORD=\"%s\"\n" +
                    "export ORCHESTRATOR_VM_ID=\"%s\"\n",
                nodePassword,
                nodeId
            );
            final String orchestratorScript = createContextualizationDataPlus(run, user, configuration, extraLines);
            System.out.println("--==== INITIAL SCRIPT ========");


            final SshClient sshClient = getSSHClient(nodeId, nodeUsername, nodePassword);
            System.out.println("sshClient = " + sshClient);
            try {
                sshClient.connect();
                System.out.println("!!! Executing script");
                ExecResponse response = sshClient.exec(orchestratorScript);
                System.out.println("response = " + response);
            }
            finally {
                try { if(sshClient != null) sshClient.disconnect(); }
                catch(Exception e) { e.printStackTrace(); }
            }
        }
        catch(com.google.common.util.concurrent.UncheckedExecutionException e) {
            e.printStackTrace();
            if(e.getCause() != null && e.getCause().getCause() != null && e.getCause().getCause().getClass() == org.jclouds.rest.AuthorizationException.class){
                throw new ServerExecutionEnginePluginException("Authorization exception for the cloud: " + getConnectorInstanceName() + ". Please check your credentials.");
            }else if(e.getCause() != null && e.getCause().getCause() != null && e.getCause().getCause().getClass() == java.lang.IllegalArgumentException.class){
                throw new ServerExecutionEnginePluginException("Error setting execution instance for the cloud " + getConnectorInstanceName() + ": " + e.getCause().getCause().getMessage());
            }else{
                throw new ServerExecutionEnginePluginException(e.getMessage());
            }
        }
        catch(SlipStreamException e) {
            e.printStackTrace();
            throw (new ServerExecutionEnginePluginException(
                "Error setting execution instance for the cloud " + getConnectorInstanceName() + ": " + e.getMessage()));
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new ServerExecutionEnginePluginException(e.getMessage());
        }
        finally {
            closeContext();
        }
    }

    protected SshClient getSSHClient(String nodeId, String nodeUsername, String nodePassword) {
        final NodeMetadata baseNodeMetadata = getComputeService().getNodeMetadata(nodeId);
        final NodeMetadata nodeMetadata = NodeMetadataBuilder.fromNodeMetadata(baseNodeMetadata).
            credentials(LoginCredentials.fromCredentials(new Credentials(nodeUsername, nodePassword))).
            build();
        final Utils sshUtils = computeServiceContext.getUtils();

        final Function<NodeMetadata,SshClient> sshClientFunction = sshUtils.sshForNode();
        final SshClient sshClient = sshClientFunction.apply(nodeMetadata);

        return sshClient;
    }

    // Copied from ConnectorBase to inject some debugging
    // There is a bug here, no id is found.
    protected List<String> getCloudNodeInstanceIds(Run run)
        throws NotFoundException, ValidationException {
        System.out.println("++getCloudNodeInstanceIds()");
        List<String> ids = new ArrayList<String>();

        for (String nodeName : run.getNodeNameList()) {
            nodeName = nodeName.trim();
            System.out.println("nodeName = " + nodeName);

            String idKey = nodeName + RuntimeParameter.NODE_PROPERTY_SEPARATOR
                + RuntimeParameter.INSTANCE_ID_KEY;
            System.out.println("idKey = " + idKey);

            String cloudServiceKey = nodeName
                + RuntimeParameter.NODE_PROPERTY_SEPARATOR
                + RuntimeParameter.CLOUD_SERVICE_NAME;
            System.out.println("cloudServiceKey = " + cloudServiceKey);

            String id = (String) run.getRuntimeParameterValueIgnoreAbort(idKey);
            System.out.println("id = " + id);
            String cloudService = run
                .getRuntimeParameterValueIgnoreAbort(cloudServiceKey);
            System.out.println("cloudService = " + cloudService);

            if (id != null
                && this.getConnectorInstanceName().equals(cloudService)) {
                ids.add(id);
            }
        }
        System.out.println("--getCloudNodeInstanceIds()");
        return ids;
    }

    public void terminate(Run run, User user) throws SlipStreamException {
        System.out.println("++terminate()");
        NovaApi client = getClient(user);

        Configuration configuration = Configuration.getInstance();
        String region = configuration
            .getRequiredProperty(constructKey("cloud.connector.service.region"));
        System.out.println("region = " + region);

        final List<String> cloudNodeInstanceIds = getCloudNodeInstanceIds(run);
        if(cloudNodeInstanceIds.isEmpty()) {
            System.out.println("cloudNodeInstanceIds is empty");
        }
        for (String instanceId : cloudNodeInstanceIds) {
            if (instanceId == null) {
                continue;
            }
            System.out.println("Deleting instanceId = '" + instanceId + "'");
            client.getServerApiForZone(region).delete(instanceId);
        }

        System.out.println("--terminate()");
    }
}
