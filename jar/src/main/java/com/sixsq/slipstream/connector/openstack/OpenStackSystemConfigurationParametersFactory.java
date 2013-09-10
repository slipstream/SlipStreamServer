package com.sixsq.slipstream.connector.openstack;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

import com.sixsq.slipstream.connector.SystemConfigurationParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;

public class OpenStackSystemConfigurationParametersFactory extends
		SystemConfigurationParametersFactoryBase {

	public OpenStackSystemConfigurationParametersFactory(String connectorInstanceName)
			throws ValidationException {
		super(connectorInstanceName);
	}
	
	@Override
	protected void initReferenceParameters() throws ValidationException {
		super.initReferenceParameters();
		super.putMandatoryEndpoint();
		super.putMandatoryOrchestrationImageId();
		
		putMandatoryParameter(constructKey(OpenStackUserParametersFactory.ORCHESTRATOR_INSTANCE_TYPE_PARAMETER_NAME),
				"OpenStack Flavor for the orchestrator. " +
				"The actual image should support the desired Flavor.");

		putMandatoryParameter(constructKey(OpenStackUserParametersFactory.SERVICE_TYPE_PARAMETER_NAME), 
				"Type-name of the service who provide the instances functionality.",
				"compute");
		
		putMandatoryParameter(constructKey(OpenStackUserParametersFactory.SERVICE_NAME_PARAMETER_NAME), 
				"Name of the service who provide the instances functionality",
				"nova");
				//"('nova' for OpenStack essex&folsom and 'Compute' for HP Cloud)"
		
		putMandatoryParameter(constructKey(OpenStackUserParametersFactory.SERVICE_REGION_PARAMETER_NAME), 
				"Region used by this cloud connector", "RegionOne");

	}

}
