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

import java.util.regex.Pattern;

import com.sixsq.slipstream.exceptions.ValidationException;

import org.simpleframework.xml.Root;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Root(name = "parameter")
@SuppressWarnings("serial")
public class ServiceCatalogParameter extends Parameter {

	@Id
	@GeneratedValue
	Long id;

	public ServiceCatalogParameter(String name, String value, String description) throws ValidationException {
		super(name, value, description);
	}

	public ServiceCatalogParameter(ServiceCatalogParameter serviceCatalogParameter) throws ValidationException {
		super(serviceCatalogParameter);
	}

	@Override
	protected void validateName() throws ValidationException {
		super.validateName();

		if ("".equals(getName())) {
			throw (new ValidationException("Error creating new Parameter, argument name cannot be empty"));
		}

		String key = getName();
		if (!Pattern.matches("\\w[-\\w\\d.]+", key)) {
			throw new ValidationException(
					"Service catalog entry must start with a letter and contain only letters, digits and dots.");
		}

	}

	@Override
	public Parameter copy() throws ValidationException {
		Parameter copy = new ServiceCatalogParameter(this);
		return copyTo(copy);
	}
}
