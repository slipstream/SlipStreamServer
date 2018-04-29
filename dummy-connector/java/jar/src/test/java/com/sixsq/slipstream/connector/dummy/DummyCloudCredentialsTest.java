package com.sixsq.slipstream.connector.dummy;

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
import com.sixsq.slipstream.connector.CloudCredentialsTestBase;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.QuotaParameter;
import com.sixsq.slipstream.persistence.UserParameter;

import java.util.HashMap;
import java.util.Map;

public class DummyCloudCredentialsTest extends CloudCredentialsTestBase {

	@Override
	public Map<String, UserParameter> createAndStoreCloudCredentials() throws ValidationException {

		Map<String, String> paramsKeyVal = new HashMap<>();
		paramsKeyVal.put(UserParametersFactoryBase.KEY_PARAMETER_NAME, "key");
		paramsKeyVal.put(UserParametersFactoryBase.SECRET_PARAMETER_NAME, "secret");
		paramsKeyVal.put(QuotaParameter.QUOTA_VM_PARAMETER_NAME, "7");
		paramsKeyVal.put(DummyUserParametersFactory.KEY_DOMAIN_NAME, "dn");

		Map<String, UserParameter> params = new HashMap<>();
		UserParameter param;
		String pname;
		for (String k: paramsKeyVal.keySet()) {
        pname = UserParameter.constructKey(getConnectorName(), k);
			param = new UserParameter(pname, paramsKeyVal.get(k), "");
			param.setCategory(getConnectorName());
			user.setParameter(param);
			params.put(pname, param);
		}

		user.store();

		return params;
	}

	@Override
	public String getCloudServiceName() {
		return DummyConnector.CLOUD_SERVICE_NAME;
	}

	@Override
	public SystemConfigurationParametersFactoryBase getSystemConfParams()
			throws ValidationException {
      return new DummySystemConfigurationParametersFactory(getConnectorName());
	}
}

