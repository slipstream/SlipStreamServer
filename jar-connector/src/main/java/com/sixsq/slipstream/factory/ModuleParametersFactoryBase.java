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

import java.util.HashMap;
import java.util.Map;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ModuleParameter;

public abstract class ModuleParametersFactoryBase extends
		ParametersFactoryBase<ModuleParameter> {

	protected Map<String, ModuleParameter> referenceParameters = new HashMap<String, ModuleParameter>();

	protected Map<String, ModuleParameter> getReferenceParameters() {
		return referenceParameters;
	}

	public ModuleParametersFactoryBase(String category)
			throws ValidationException {
		super(category);
		initReferenceParameters();
	}

	public Map<String, ModuleParameter> getParameters() {
		return referenceParameters;
	}

	protected ModuleParameter createParameter(String name, String description,
			boolean mandatory) throws ValidationException {
		return createParameter(name, null, description, mandatory);
	}

	protected ModuleParameter createParameter(String name, boolean value,
			String description) throws ValidationException {
		ModuleParameter p = new ModuleParameter(constructKey(name), value ? "on"
				: null, description);
		p.setCategory(getCategory());
		p.setMandatory(true);
		return p;
	}

	protected ModuleParameter createParameter(String name, String value,
			String description, boolean mandatory) throws ValidationException {
		ModuleParameter p = new ModuleParameter(constructKey(name), value,
				description);
		p.setName(p.getName());
		p.setCategory(getCategory());
		p.setMandatory(true);
		return p;
	}

	protected void addSecurityGroupsParameter() throws ValidationException {
		putMandatoryParameter(SECURITY_GROUPS_PARAMETER_NAME, "Security Groups (comma separated list)",
				SECURITY_GROUPS_ALLOW_ALL,
				"If you don't know what is a security group or if you don't want to take care of them, you can use the "
				+ "special value <code>" + SECURITY_GROUPS_ALLOW_ALL + "</code> to let SlipStream generate a security group with "
				+ "everything allowed.");
	}

}
