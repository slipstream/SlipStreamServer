package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.connector.CredentialsBase;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;

public class CloudStackCredentials extends CredentialsBase {

	public CloudStackCredentials(User user, String connectorInstanceName) {
		super(user);
		try {
			cloudParametersFactory = new CloudStackUserParametersFactory(connectorInstanceName);
		} catch (ValidationException e) {
			e.printStackTrace();
			throw (new SlipStreamRuntimeException(e));
		}
	}

	@Override
	public String getKey() throws InvalidElementException {
		return getParameterValue(CloudStackUserParametersFactory.KEY_PARAMETER_NAME);
	}

	@Override
	public String getSecret() throws InvalidElementException {
		return getParameterValue(CloudStackUserParametersFactory.SECRET_PARAMETER_NAME);
	}

}
