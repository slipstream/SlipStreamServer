package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.connector.SystemConfigurationParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;

public class CloudStackSystemConfigurationParametersFactory extends
		SystemConfigurationParametersFactoryBase {	
	
	public CloudStackSystemConfigurationParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {

		super.initReferenceParameters();

		putMandatoryEndpoint();

		putMandatoryParameter(constructKey(CloudStackUserParametersFactory.ORCHESTRATOR_INSTANCE_TYPE_PARAMETER_NAME),
				"Orchestrator instance type");
		
		putMandatoryParameter(constructKey(CloudStackUserParametersFactory.ZONE_PARAMETER_NAME), 
				"Zone");
		
		//putMandatoryParameter(constructKey(CloudStackUserParametersFactory.UPDATE_CLIENTURL_PARAMETER_NAME),
		//		"URL with the cloud client specific connector");
	}
	
}
