package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;

public class CloudStackUserParametersFactory extends UserParametersFactoryBase {

	public static String ZONE_PARAMETER_NAME = "zone";
	
	public CloudStackUserParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		putMandatoryParameter(KEY_PARAMETER_NAME, "Key");
		putMandatoryPasswordParameter(SECRET_PARAMETER_NAME, "Secret");
	}

}
