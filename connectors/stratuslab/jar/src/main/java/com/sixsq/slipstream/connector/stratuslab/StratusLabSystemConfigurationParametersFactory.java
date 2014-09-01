package com.sixsq.slipstream.connector.stratuslab;

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

public class StratusLabSystemConfigurationParametersFactory extends
		SystemConfigurationParametersFactoryBase {

	public StratusLabSystemConfigurationParametersFactory(
			String connectorInstanceName) throws ValidationException {
		super(connectorInstanceName);
	}

	protected void initReferenceParameters() throws ValidationException {

		super.initReferenceParameters();

		putMandatoryEndpoint();

		putMandatoryParameter(constructKey(StratusLabUserParametersFactory.ORCHESTRATOR_INSTANCE_TYPE_PARAMETER_NAME),
				"Orchestrator instance type");

		putMandatoryParameter(constructKey(StratusLabUserParametersFactory.MARKETPLACE_ENDPOINT_PARAMETER_NAME),
				"Marketplace endpoint");

		putMandatoryParameter(constructKey(StratusLabUserParametersFactory.PDISK_ENDPOINT_PARAMETER_NAME),
				"PDisk endpoint");

		putMandatoryParameter(constructKey(StratusLabUserParametersFactory.UPDATE_CLIENTURL_PARAMETER_NAME),
				"URL with the cloud client specific connector");
	}

}
