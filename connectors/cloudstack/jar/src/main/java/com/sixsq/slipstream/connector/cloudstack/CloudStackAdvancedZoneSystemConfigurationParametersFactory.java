package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.exceptions.ValidationException;

public class CloudStackAdvancedZoneSystemConfigurationParametersFactory extends
		CloudStackSystemConfigurationParametersFactory {	
	
	public static final String ORCHESTRATOR_NETWORKS = "orchestrator.networks";
	
	public CloudStackAdvancedZoneSystemConfigurationParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {

		super.initReferenceParameters();
		
		putMandatoryParameter(constructKey(ORCHESTRATOR_NETWORKS),
				"List of networks for the Orchestrator (comma separated)");
	}
	
}
