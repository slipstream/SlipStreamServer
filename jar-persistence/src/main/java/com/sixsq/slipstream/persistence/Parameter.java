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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.exceptions.ValidationException;

import flexjson.JSON;

/**
 * Unit test see
 *
 * @see ParameterTest
 *
 */
@MappedSuperclass
@SuppressWarnings("serial")
@Root(name = "parameter")
public abstract class Parameter<T> implements Serializable {

	private static final CharSequence INVALID_RESTRICTED_CHAR = "'";

	@ManyToOne
	@JSON(include=false)
	private T container;

	@Attribute
	private String name;

	@Column(length = 65536)
	private String value;

	@Attribute(required = false)
	@Column(length = 1024)
	private String description;

	@Attribute(required = false)
	private String category;

	@Attribute(required = false)
	@Transient
	private transient String inheritedFromUri;

	@Attribute(required = false)
	private boolean mandatory = false;

	@Attribute(required = false)
	private ParameterType type = ParameterType.String;

	@Attribute(required = false)
	private boolean readonly;

	@Element(data = true, required = false)
	@Column(length = 65536)
	private String instructions = null;

	@ElementArray(required = false)
	private String[] enumValues;

	@Attribute(required = false)
	private Integer order_ = 0;

	protected Parameter() {
	}

	public Parameter(String name) throws ValidationException {
		this();
		this.name = name;
		validateName();
		this.category = ParameterCategory.General.name();
	}

	public Parameter(String name, String value, String description)
			throws ValidationException {
		this(name);
		this.description = description;
		this.value = value;
	}

	protected void validateName() throws ValidationException {
		if (name == null) {
			throw (new ValidationException(
					"Error creating new Parameter, argument name cannot be null"));
		}
	}

	abstract public Long getId();

	abstract protected void setId(Long id);

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Element(required = false, data = true)
	public String getValue() {
		return value;
	}

	public String getValue(String defaultValue) {
		return hasValueSet() ? value : defaultValue;
	}

	public String getSafeValue() {
		return "'" + value + "'";
	}

	@Element(required = false, data = true)
	public void setValue(String value) throws ValidationException {
		if (type == ParameterType.Boolean) {
			this.value = Boolean.toString(isTrue(value));
		} else {
			this.value = value;
		}
	}

	public void validateValue() throws ValidationException {
		if (type == ParameterType.Enum) {
			validateEnum(value);
		}
		if (isRestrictedValue()) {
			if (value != null && value.contains(INVALID_RESTRICTED_CHAR)) {
				throw (new ValidationException("Invalid character ("
						+ INVALID_RESTRICTED_CHAR + ") in parameter: " + name));
			}
		}
	}

	private void validateEnum(String value) throws ValidationException {
		boolean found = false;

		if (value == null || enumValues == null || "".equals(value)) {
			return;
		}
		for (String v : enumValues) {
			if (v.equals(value)) {
				found = true;
				break;
			}
		}
		if (!found) {
			throw new ValidationException("Invalid enumeration value: " + value);
		}
	}

	public boolean hasValueSet() {
		return hasValueSet(value);
	}

	public static boolean hasValueSet(String value) {
		return !(value == null || "".equals(value));
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getInheritedFromUri() {
		return inheritedFromUri;
	}

	public void setInheritedFromUri(String inheritedFrom) {
		this.inheritedFromUri = inheritedFrom;
	}

	final public T getContainer() {
		return container;
	}

	final public void setContainer(T container) {
		this.container = container;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(ParameterCategory category) {
		this.category = category.name();
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public ParameterType getType() {
		return type;
	}

	public void setType(ParameterType type) {
		this.type = type;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setEnumValues(List<String> enumValues) {
		type = ParameterType.Enum;
		if (enumValues == null) {
			this.enumValues = null;
		} else {
			this.enumValues = enumValues.toArray(new String[0]);
		}
	}

	public List<String> getEnumValues() {
		return (enumValues == null) ? null : Arrays.asList(enumValues);
	}

	@Attribute(required = false)
	public int getOrder() {
		if (order_ == null) {
			return 0;
		}
		return order_;
	}

	@Attribute(required = false)
	public void setOrder(int order) {
		this.order_ = order;
	}

	protected boolean isRestrictedValue() {
		return getType() == ParameterType.RestrictedText
				|| getType() == ParameterType.RestrictedString
				|| getType() == ParameterType.Password;
	}

	abstract public Parameter<T> copy() throws ValidationException;

	public Parameter<T> copyTo(Parameter<T> copy) throws ValidationException {

		copy.setCategory(getCategory());
		copy.setEnumValues(getEnumValues());
		copy.setInheritedFromUri(getInheritedFromUri());
		copy.setInstructions(getInstructions());
		copy.setMandatory(isMandatory());
		copy.setReadonly(isReadonly());
		copy.setType(getType());

		return copy;
	}

	public boolean isTrue() {
		return isTrue(getValue());
	}

	public static boolean isTrue(String value) {
		return Boolean.parseBoolean(value);
	}

	public static String constructKey(String category, String... names) {
		StringBuilder newKey = new StringBuilder(category);
		for (String name : names) {
			newKey.append(RuntimeParameter.PARAM_WORD_SEPARATOR).append(name);
		}
		return newKey.toString();
	}

	@Override
	public String toString() {
		return value;
	}

}
