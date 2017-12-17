package com.sixsq.slipstream.factory;

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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterType;
import com.sixsq.slipstream.persistence.QuotaParameter;
import com.sixsq.slipstream.ssclj.util.UserParamsDesc;

import java.util.List;
import java.util.Map;

public abstract class ParametersFactoryBase<S extends Parameter<?>> {

	public static final String SECURITY_GROUPS_PARAMETER_NAME = "security.groups";
	public static final String SECURITY_GROUPS_ALLOW_ALL = "slipstream_managed";

	private String category;

	protected abstract void initReferenceParameters()
			throws ValidationException;

	protected void initReferenceParameters(String cloudName) throws ValidationException {
		Map<String, Map> paramsDesc = UserParamsDesc.getCloudDesc(cloudName);
		initReferenceParameters(paramsDesc);
	}

	protected String descKeyToParamName(String key) {
		return key;
	}

	private ParameterType descTypeToParamType(String type) {
		ParameterType ptype;
		switch (type) {
			case "password":
				ptype = ParameterType.Password;
				break;
			case "restricted text":
				ptype = ParameterType.RestrictedText;
				break;
			default:
				ptype = ParameterType.String;
				break;
		}
		return ptype;
	}

	private boolean skipParameter(String name) throws ValidationException {
		switch (name) {
			case QuotaParameter.QUOTA_VM_PARAMETER_NAME:
				return !Configuration.isQuotaEnabled();
			default:
				return false;
		}
	}

	private String defaultParamValue(String name) throws ValidationException {
		switch (name) {
			case QuotaParameter.QUOTA_VM_PARAMETER_NAME:
				return QuotaParameter.QUOTA_VM_DEFAULT;
			default:
				return "";
		}
	}

	protected void initReferenceParameters(Map<String, Map> paramsDesc)
			throws ValidationException {
		for (String key : paramsDesc.keySet()) {
			Map desc = paramsDesc.get(key);

			String name = descKeyToParamName(key);
			if (skipParameter(name)) {
				continue;
			}
			String description = (String) desc.get("displayName");
			Boolean mandatory = (Boolean) desc.get("mandatory");
			String value = defaultParamValue(name);

			S parameter = createParameter(name, value, description, mandatory);

			String instructions = (String) desc.get("description");
			int order = ((Long) desc.get("order")).intValue();
			ParameterType ptype = descTypeToParamType((String) desc.get("type"));
			Boolean readOnly = (Boolean) desc.get("readOnly");

			parameter.setInstructions(instructions);
			parameter.setOrder(order);
			parameter.setType(ptype);
			parameter.setReadonly(readOnly);

			assignParameter(parameter);
		}
	};

	public ParametersFactoryBase(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}

	protected abstract Map<String, S> getReferenceParameters();

	protected void putParameter(String name, String description,
			ParameterType type, boolean mandatory) throws ValidationException {
		S parameter = createParameter(name, null, description, mandatory);
		parameter.setType(type);
		assignParameter(parameter);
	}

	protected abstract S createParameter(String name, String value,
			String description, boolean mandatory) throws ValidationException;

	protected abstract S createParameter(String name, String description,
			boolean mandatory) throws ValidationException;

	protected abstract S createParameter(String name, boolean value,
			String description) throws ValidationException;

	protected void addParameter(S parameter, ParameterType type,
			boolean mandatory) {
		parameter.setType(type);
		parameter.setMandatory(mandatory);
		assignParameter(parameter);
	}

	protected void putTextParameter(String name, String description,
			boolean mandatory) throws ValidationException {
		S parameter = createParameter(name, description, mandatory);
		addParameter(parameter, ParameterType.Text, mandatory);
	}

	protected void putEnumParameter(String name, String description,
			List<String> options, String value, boolean mandatory)
			throws ValidationException {
		putEnumParameter(name, description, options, value, mandatory, 0);
	}

	protected void putEnumParameter(String name, String description,
			List<String> options, String value, boolean mandatory, int order)
			throws ValidationException {
		S parameter = createParameter(name, description, mandatory);
		parameter.setEnumValues(options);
		parameter.setValue(value);
		parameter.setOrder(order);
		addParameter(parameter, ParameterType.Enum, mandatory);
	}

	protected void putEnumParameter(String name, String description,
			List<String> options, String value, String instructions, boolean mandatory, int order)
			throws ValidationException {
		S parameter = createParameter(name, description, mandatory);
		parameter.setEnumValues(options);
		parameter.setValue(value);
		parameter.setOrder(order);
		parameter.setInstructions(instructions);
		addParameter(parameter, ParameterType.Enum, mandatory);
	}

	protected void putMandatoryEnumParameter(String name, String description,
			List<String> options, String value) throws ValidationException {
		putEnumParameter(name, description, options, value, true);
	}

	protected void putMandatoryEnumParameter(String name, String description,
			List<String> options, String value, int order)
			throws ValidationException {
		putEnumParameter(name, description, options, value, true, order);
	}

	protected void putMandatoryEnumParameter(String name, String description,
			List<String> options, String value, String instructions, int order)
			throws ValidationException {
		putEnumParameter(name, description, options, value, instructions, true, order);
	}

	protected void putParameter(String name, boolean value, String description,
			boolean mandatory) throws ValidationException {
		S parameter = createParameter(name, value, description);
		addParameter(parameter, ParameterType.Boolean, mandatory);
	}

	protected void putParameter(String name, String description,
			boolean mandatory) throws ValidationException {
		S parameter = createParameter(name, null, description, mandatory);
		assignParameter(parameter);
	}

	protected void putMandatoryBooleanParameter(String name, boolean value,
			String description, int order) throws ValidationException {
		S parameter = createParameter(name, value, description);
		parameter.setOrder(order);
		addParameter(parameter, ParameterType.Boolean, true);
	}

	protected void putMandatoryBooleanParameter(String name, boolean value,
			String description, String instructions, int order) throws ValidationException {
		S parameter = createParameter(name, value, description);
		parameter.setOrder(order);
		parameter.setInstructions(instructions);
		addParameter(parameter, ParameterType.Boolean, true);
	}

	protected void putMandatoryParameter(String name, String description,
			ParameterType type) throws ValidationException {
		S parameter = createParameter(name, null, description, true);
		parameter.setType(type);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description,
			ParameterType type, int order) throws ValidationException {
		S parameter = createParameter(name, null, description, true);
		parameter.setType(type);
		parameter.setOrder(order);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description,
			String value, int order) throws ValidationException {
		S parameter = createParameter(name, null, description, true);
		parameter.setOrder(order);
		parameter.setValue(value);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description,
			String value, String instructions) throws ValidationException {
		S parameter = createParameter(name, null, description, true);
		parameter.setValue(value);
		parameter.setInstructions(instructions);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description,
										 String value, String instructions, int order) throws ValidationException {
		S parameter = createParameter(name, null, description, true);
		parameter.setValue(value);
		parameter.setInstructions(instructions);
		parameter.setOrder(order);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description,
			int order) throws ValidationException {
		S parameter = createParameter(name, null, description, true);
		parameter.setOrder(order);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description,
			ParameterType type, String instructions) throws ValidationException {
		S parameter = createParameter(name, null, description, true);
		parameter.setType(type);
		parameter.setInstructions(instructions);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description,
			ParameterType type, String instructions, int order) throws ValidationException {
		S parameter = createParameter(name, null, description, true);
		parameter.setType(type);
		parameter.setInstructions(instructions);
		parameter.setOrder(order);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description, String value,
			ParameterType type, String instructions, int order) throws ValidationException {
		S parameter = createParameter(name, null, description, true);
		parameter.setType(type);
		parameter.setInstructions(instructions);
		parameter.setOrder(order);
		parameter.setValue(value);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description,
			String value) throws ValidationException {
		S parameter = createParameter(name, value, description, true);
		assignParameter(parameter);
	}

	protected void putMandatoryParameter(String name, String description)
			throws ValidationException {
		putMandatoryParameter(name, description, "");
	}

	protected void putParameter(String name, String description,
			boolean mandatory, ParameterType type) throws ValidationException {
		S parameter = createParameter(name, null, description, mandatory);
		parameter.setType(type);
		assignParameter(parameter);
	}

	protected void putParameter(String name, String description,
			String instructions, ParameterType type, boolean mandatory)
			throws ValidationException {
		S parameter = createParameter(name, null, description, mandatory);
		parameter.setType(type);
		parameter.setInstructions(instructions);
		assignParameter(parameter);
	}

	protected void putParameter(String name, String value, String description,
			String instructions, boolean mandatory) throws ValidationException {
		S parameter = createParameter(name, value, description, mandatory);
		parameter.setInstructions(instructions);
		assignParameter(parameter);
	}

	protected void putParameter(String name, String value, String description,
			boolean mandatory) throws ValidationException {
		S parameter = createParameter(name, value, description, mandatory);
		assignParameter(parameter);
	}

	protected void putParameter(String name, String value, String description,
			boolean mandatory, boolean readonly) throws ValidationException {
		S parameter = createParameter(name, value, description, mandatory);
		parameter.setReadonly(readonly);
		assignParameter(parameter);
	}

	protected void putMandatoryPasswordParameter(String name, String description)
			throws ValidationException {
		S parameter = createParameter(name, description, true);
		addParameter(parameter, ParameterType.Password, true);
	}

	protected void putMandatoryPasswordParameter(String name,
			String description, int order) throws ValidationException {
		S parameter = createParameter(name, description, true);
		parameter.setOrder(order);
		addParameter(parameter, ParameterType.Password, true);
	}

	protected void putMandatoryPasswordParameter(String name,
			String description, String instructions) throws ValidationException {
		S parameter = createParameter(name, description, true);
		parameter.setInstructions(instructions);
		addParameter(parameter, ParameterType.Password, true);
	}

	protected void putMandatoryPasswordParameter(String name,
			String description, String instructions, int order) throws ValidationException {
		S parameter = createParameter(name, description, true);
		parameter.setOrder(order);
		parameter.setInstructions(instructions);
		addParameter(parameter, ParameterType.Password, true);
	}

	protected void assignParameter(S parameter) {
		getReferenceParameters().put(parameter.getName(), parameter);
	}

	public String constructKey(String... names) {
		return Parameter.constructKey(getCategory(), names);
	}
}