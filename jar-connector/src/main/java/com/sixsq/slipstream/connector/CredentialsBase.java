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

import com.sixsq.slipstream.credentials.CloudCredential;
import com.sixsq.slipstream.credentials.CloudCredentialCreateTmpl;
import com.sixsq.slipstream.credentials.Credentials;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

import java.util.Map;

public abstract class CredentialsBase implements Credentials {

	@Override
	abstract public String getKey() throws InvalidElementException;

	@Override
	abstract public String getSecret() throws InvalidElementException;

	protected User user;
	protected UserParametersFactoryBase cloudParametersFactory;

	public CredentialsBase(User user) {
		this.user = user;
	}

	@Override
	public void validate() throws ValidationException {
		try {
			if(!Parameter.hasValueSet(getKey())) {
				throw new ValidationException("Invalid credentials: missing key");
			}
			if(!Parameter.hasValueSet(getSecret())) {
				throw new ValidationException("Invalid credentials: missing secret");
			}
		} catch (InvalidElementException e) {
			throw new ValidationException(e.getMessage());
		}
	}
	
	protected String getParameterValue(String key) throws InvalidElementException {
		UserParameter parameter = user.getParameter(qualifyKey(key));
		if (parameter == null) {
			throwInvalidElementException(key);
		}
		return parameter.getValue();
	}

	private String qualifyKey(String key) {
		return cloudParametersFactory.constructKey(key);
	}

	private void throwInvalidElementException(String key)
			throws InvalidElementException {
		throw (new InvalidElementException("Missing mandatory user parameter: "
				+ key + ". Consider editing your <a href='" + "/user/"
				+ user.getName() + "'>user account</a>"));
	}

	/**
	 *
	 * When mapping is not known, null should be returned.
	 * @param pName parameter name of the UserParameter.
	 * @param cloudCredsJSON JSON with cloud cred definition document.
	 * @return value for the parameter or null
	 * @throws ValidationException
	 */
	protected String getCloudCredParamValue(String pName, String cloudCredsJSON) throws ValidationException {
		return null;
	}

	public Map<String, UserParameter> setUserParametersValues(
			String cloudCredsJSON) throws ValidationException {
		Map<String, UserParameter> paramsMap = cloudParametersFactory.getParameters();
		for (UserParameter p: paramsMap.values()) {
			String paramName = p.getName();
			String name = paramName.replace(p.getCategory() + ".", "");
			// the type of the value of any parameter is String.
			String value = getCloudCredParamValue(name, cloudCredsJSON);
			if (null != value) {
				p.setValue(value);
				paramsMap.put(paramName, p);
			}
		}
		return paramsMap;
	}

	public CloudCredentialCreateTmpl getCloudCredCreateTmpl(Map<String, UserParameter> params, String connInstanceName) {
		if (params.size() < 1) {
			return null;
		}
		return new CloudCredentialCreateTmpl(getCloudCredential(params, connInstanceName));
	}

	public void store() throws ValidationException {
		String category = cloudParametersFactory.getCategory();
		Map<String, UserParameter> params = this.user.getParameters(category);
		if (null == params || params.size() < 1) {
			return;
		}
		CloudCredential cd = (CloudCredential) getCloudCredential(params, category);
		if (null != cd) {
			cd.store(this.user);
		}
	}
}
