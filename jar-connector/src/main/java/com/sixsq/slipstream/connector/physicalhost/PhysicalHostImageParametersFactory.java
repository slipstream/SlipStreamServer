package com.sixsq.slipstream.connector.physicalhost;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.ModuleParametersFactoryBase;
import com.sixsq.slipstream.persistence.ParameterType;

public class PhysicalHostImageParametersFactory extends ModuleParametersFactoryBase {

	public static String SECRET_PARAMETER_NAME = "password";
	public static String PRIVATE_KEY = "private.key";
	
	public PhysicalHostImageParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		putParameter(SECRET_PARAMETER_NAME, "Password", "You need to provide at least a password or a private key. If you provide a password and a private key, the password will be used as password for private key.", ParameterType.Password, false);
		putParameter(PRIVATE_KEY, "Private key", "You need to provide at least a password or a private key. If you provide a password and a private key, the password will be used as password for private key.", ParameterType.Text, false);
	}

}
