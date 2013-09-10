package com.sixsq.slipstream.connector;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.UserParameter;

public abstract class UserParametersFactoryBase extends
		ParametersFactoryBase<UserParameter> {

	public static String KEY_PARAMETER_NAME = "username";
	public static String SECRET_PARAMETER_NAME = "password";
	public static String SSHKEY_PARAMETER_NAME = "ssh.public.key";
	public static String DEFAULT_CLOUD_SERVICE_PARAMETER_NAME = "default.cloud.service";
	public static final String ENDPOINT_PARAMETER_NAME = "endpoint";
	public static final String ORCHESTRATOR_IMAGEID_PARAMETER_NAME = "orchestrator.imageid";
	public static final String ORCHESTRATOR_INSTANCE_TYPE_PARAMETER_NAME = "orchestrator.instance.type";
	public static final String UPDATE_CLIENTURL_PARAMETER_NAME = "update.clienturl";
	
	protected Map<String, UserParameter> referenceParameters = new HashMap<String, UserParameter>();

	public static List<String> extractCloudNames(
			Map<String, Connector> connectors) {
		List<String> names = new ArrayList<String>();
		for (Connector c : connectors.values()) {
			names.add(c.getConnectorInstanceName());
		}
		return names;
	}

	protected Map<String, UserParameter> getReferenceParameters() {
		return referenceParameters;
	}

	public UserParametersFactoryBase(String category)
			throws ValidationException {
		super(category);
		initReferenceParameters();
	}

	public Map<String, UserParameter> getParameters() {
		return referenceParameters;
	}

	protected UserParameter createParameter(String name, String description,
			boolean mandatory) {
		return createParameter(name, null, description, mandatory);
	}

	protected UserParameter createParameter(String name, boolean value,
			String description) {
		UserParameter p = new UserParameter(constructKey(name),
				String.valueOf(value), description);
		p.setCategory(getCategory());
		p.setMandatory(true);
		return p;
	}

	protected UserParameter createParameter(String name, String value,
			String description, boolean mandatory) {
		UserParameter p = new UserParameter(constructKey(name), value,
				description);
		p.setName(p.getName());
		p.setCategory(getCategory());
		p.setMandatory(true);
		return p;
	}
	
	public static String getPublicKeyParameterName(){
		return ParametersFactory.constructKey(ParameterCategory.General.toString(), SSHKEY_PARAMETER_NAME);
	}

}
