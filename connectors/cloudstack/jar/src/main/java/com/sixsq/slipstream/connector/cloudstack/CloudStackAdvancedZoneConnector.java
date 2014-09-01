package com.sixsq.slipstream.connector.cloudstack;

import java.util.Map;

import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;

public class CloudStackAdvancedZoneConnector extends CloudStackConnector {

	public static final String ZONE_TYPE = "Advanced";
	public static final String NEW_CLOUDCONNECTOR_PYTHON_MODULENAME = "slipstream.cloudconnectors.cloudstack.CloudStackAdvancedZoneClientCloud";

    public static final String CLOUD_SERVICE_NAME = "cloudstackadvancedzone";

	public CloudStackAdvancedZoneConnector() {
		this(CLOUD_SERVICE_NAME);
		this.CLOUDCONNECTOR_PYTHON_MODULENAME = NEW_CLOUDCONNECTOR_PYTHON_MODULENAME;
	}

	public CloudStackAdvancedZoneConnector(String instanceName) {
		super(instanceName);
		this.CLOUDCONNECTOR_PYTHON_MODULENAME = NEW_CLOUDCONNECTOR_PYTHON_MODULENAME;
	}

	@Override
	public Connector copy(){
    	return new CloudStackAdvancedZoneConnector(getConnectorInstanceName());
    }

	@Override
	protected String getZoneType(){
		return ZONE_TYPE;
	}

    @Override
    public String getCloudServiceName() {
        return CLOUD_SERVICE_NAME;
    }

/*
	@Override
	protected void validateCapabilities(Run run) throws SlipStreamException {
		return;
	}
*/

	@Override
	protected String getNetworks(Run run, User user) throws ValidationException{
		String networks = "";

		if (isInOrchestrationContext(run)) {
			networks = user.getParameter(constructKey(CloudStackAdvancedZoneSystemConfigurationParametersFactory.ORCHESTRATOR_NETWORKS)).getValue();
		} else {
			ImageModule machine = ImageModule.load(run.getModuleResourceUrl());
			networks = machine.getParameterValue(CloudStackAdvancedZoneImageParametersFactory.NETWORKS, null);
		}

		return networks;
	}

	@Override
	public Map<String, ModuleParameter> getImageParametersTemplate()
			throws ValidationException {
		return new CloudStackAdvancedZoneImageParametersFactory(getConnectorInstanceName())
				.getParameters();
	}

	@Override
	public Map<String, ServiceConfigurationParameter> getServiceConfigurationParametersTemplate()
			throws ValidationException {
		return new CloudStackAdvancedZoneSystemConfigurationParametersFactory(
				getConnectorInstanceName()).getParameters();
	}

}
