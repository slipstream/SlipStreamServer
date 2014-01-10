package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.ModuleParametersFactoryBase;
import com.sixsq.slipstream.persistence.ImageModule;

public class CloudStackImageParametersFactory extends ModuleParametersFactoryBase {

	public static String SECURITY_GROUPS = "security.groups";
	
	public CloudStackImageParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}

	@Override
	protected void initReferenceParameters() throws ValidationException {
		putParameter(ImageModule.INSTANCE_TYPE_KEY, "Instance type (flavor)", true);
		putMandatoryParameter(SECURITY_GROUPS, "Security Groups (comma separated list)", "default");
	}

}
