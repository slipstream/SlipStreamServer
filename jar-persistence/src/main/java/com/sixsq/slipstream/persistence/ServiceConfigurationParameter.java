package com.sixsq.slipstream.persistence;

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

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration.RequiredParameters;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.regex.Pattern;

@Entity
@Root(name = "parameter")
@SuppressWarnings("serial")
public class ServiceConfigurationParameter extends
		Parameter<ServiceConfiguration> {

	@Id
	@GeneratedValue
	Long id;

	@Attribute(required = false)
	private ServiceConfigurationParameter() {
	}

	public ServiceConfigurationParameter(String name)
			throws ValidationException {
		super(name);
	}

	public ServiceConfigurationParameter(String name, String value)
			throws ValidationException {
		super(name, value, "");
	}

	public ServiceConfigurationParameter(String name, String value,
			String description) throws ValidationException {
		super(name, value, description);
	}

	public ServiceConfigurationParameter(RequiredParameters parameter,
			String value) throws ValidationException {
		super(parameter.name(), value, parameter.getDescription());
		setType(parameter.getType());
		setInstructions(parameter.getInstruction());
		setCategory(parameter.getCategory().name());
		setMandatory(true);
		setReadonly(parameter.isReadonly());
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	protected void setId(Long id) {
		this.id = id;
	}

	@Override
	public ServiceConfigurationParameter copy() throws ValidationException {
		return (ServiceConfigurationParameter) copyTo(new ServiceConfigurationParameter(
				getName(), getValue(), getDescription()));
	}

	@Override
	protected void validateName() throws ValidationException {
		super.validateName();
		if ("".equals(getName())) {
			throw (new ValidationException("Key cannot be empty"));
		}
		if (!Pattern.matches("[a-zA-Z][\\w\\d._-]+", getName())) {
			throw new ValidationException(
					"Key must start with a letter and contain only letters, digits, dots, dash and underscores.");
		}
	}
}
