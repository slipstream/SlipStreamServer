package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;

public class CloudStackAdvancedZoneImageParametersFactory extends CloudStackImageParametersFactory {

	public static final String NETWORKS = "networks";

	public CloudStackAdvancedZoneImageParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		super.initReferenceParameters();
		putParameter(NETWORKS, "List of networks (comma separated)", true);
	}

}
