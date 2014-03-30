package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ParameterType;

public class CloudStackUserParametersFactory extends UserParametersFactoryBase {

	public static final String ZONE_PARAMETER_NAME = "zone";

	public CloudStackUserParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		putMandatoryParameter(KEY_PARAMETER_NAME, "Key",
				ParameterType.RestrictedString, 10);
		putMandatoryPasswordParameter(SECRET_PARAMETER_NAME, "Secret", 20);
	}

}
