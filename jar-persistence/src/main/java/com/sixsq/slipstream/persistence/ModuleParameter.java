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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import com.sixsq.slipstream.exceptions.ValidationException;

@Entity
@SuppressWarnings("serial")
public class ModuleParameter extends Parameter<Module> {

	private static final String INSTANCE_TYPE_INHERITED = ImageModule.INSTANCE_TYPE_INHERITED;

	@Id
	@GeneratedValue
	Long id;

	@SuppressWarnings("unused")
	private ModuleParameter() {
	}

	public ModuleParameter(String name) throws ValidationException {
		super(name);
	}

	public ModuleParameter(String name, String value, String description)
			throws ValidationException {
		super(name, value, description);
		setValue(value);
	}

	public ModuleParameter(String name, String value, String description,
			ParameterCategory category) throws ValidationException {
		super(name, value, description);
		setCategory(category);
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
	public ModuleParameter copy() throws ValidationException {
		ModuleParameter copy = new ModuleParameter(getName(), getValue(),
				getDescription());
		copy.setDefaultValue(getDefaultValue());
		return (ModuleParameter) copyTo(copy);
	}

	@Override
	public boolean hasValueSet() {
		return isSet();
	}

	@Override
	@Element(required = false, data = true)
	public void setValue(String value) throws ValidationException {
		super.setValue(value);
		isSet = hasValueSet(value) ? true : false;
	}

	@Override
	@Element(required = false, data = true)
	public String getValue() {
		String value;
		if (isSet()) {
			if (isInheritedEnumValue()) {
				try {
					value = getDefaultValueFromParent();
				} catch (ValidationException e) {
					value = null;
				}
			} else {
				value = super.getValue();
			}
		} else {
			try {
				value = getDefaultValue();
			} catch (ValidationException e) {
				value = null;
			}
		}
		return value;
	}

	private boolean isInheritedEnumValue() {
		return getType() == ParameterType.Enum
				&& INSTANCE_TYPE_INHERITED.equals(super.getValue());
	}

	@Attribute(required=false)
	private boolean isSet = false;

	public boolean isSet() {
		return isSet;
	}

	public void reset() throws ValidationException {
		super.setValue(null);
		isSet = false;
	}

	@Override
	public String getValue(String defaultValue) throws ValidationException {
		return isSet() ? super.getValue() : defaultValue;
	}

	@Element(required = false, data = true)
	@Column(length=1024)
	public void setDefaultValue(String defaultValue) {
	}

	/**
	 * Inherited from parent or current value
	 */
	@Element(required = false, data = true)
	@Column(length=1024)
	public String getDefaultValue() throws ValidationException {
		String defaultValue = null;
		String value = super.getValue();
		if (hasValueSet(value)) {
			defaultValue = value;
		} else {
			defaultValue = getDefaultValueFromParent();
		}
		return defaultValue;
	}

	private String getDefaultValueFromParent() throws ValidationException {
		String defaultValue = null;
		if (getContainer() instanceof ImageModule) {
			ImageModule parentModule = ((ImageModule) getContainer())
					.getParentModule();
			if (parentModule != null) {
				ModuleParameter parentParameter = parentModule
						.getParameter(getName());
				if (parentParameter != null) {
					defaultValue = parentParameter.getValue();
				}
			}
		}
		return defaultValue;
	}

}
