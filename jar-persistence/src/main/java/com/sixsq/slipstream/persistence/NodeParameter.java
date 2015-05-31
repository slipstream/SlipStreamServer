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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.exceptions.ValidationException;

@Root(name = "parameter")
@SuppressWarnings("serial")
public class NodeParameter extends Parameter {

	private static final Pattern NAME_VALIDATION_PATTERN = Pattern
			.compile("(.+\\" + RuntimeParameter.NODE_PROPERTY_SEPARATOR
					+ ".+)|(ss:.+)|(orchestrator(-\\w+)?:.+)|(machine:.+)|"
					+ "(.+:multiplicity)|(\"*\")|('*')");

	public static void validate(NodeParameter np) throws ValidationException {

		String value = np.getValue();
		// hack, can't get the last part of the regex to work
		if (isStringValue(value)) {
			return;
		}

		Matcher matcher = NAME_VALIDATION_PATTERN.matcher(value);

		if (!matcher.matches()) {
			throw (new ValidationException(
					"Invalid value: "
							+ value
							+ " for parameter: "
							+ np.getName()
							+ ". Should be in the form: "
							+ RuntimeParameter.GLOBAL_NAMESPACE_PREFIX
							+ "* or "
							+ Run.ORCHESTRATOR_NAME
							+ "* or "
							+ Run.MACHINE_NAME_PREFIX
							+ "* or [nodename]"
							+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
							+ "* or if you want to set it to a static value, wrap it with single or double quotes."));
		}

	}

	public static boolean isStringValue(String value) {
		boolean single = value.startsWith("\'") && value.endsWith("\'");
		boolean double_ = value.startsWith("\"") && value.endsWith("\"");
		return double_ || single;
	}

	@Id
	@GeneratedValue
	Long id;

	// Determines whether the given value is a real data value (string) or is a
	// reference key to a runtime parameter in another node.
	@Attribute
	boolean isMappedValue = false;

	@SuppressWarnings("unused")
	private NodeParameter() {
	}

	/**
	 * If the parameter is added to a node named 'node1' with an input parameter
	 * 'p1' and this parameter is to be mapped to an output parameter
	 * 'node2.1:p1' then the following parameters should take the values:
	 *
	 * @param name
	 *            for example 'p1'
	 * @param value
	 *            for example 'node2.1:p2'
	 * @param description
	 *            the description of the mapping (normally left blank)
	 * @throws ValidationException
	 */
	public NodeParameter(String name, String value, String description)
			throws ValidationException {
		super(name, value, description);
		setValue(value);
		validate(this);
	}

	/**
	 * @throws ValidationException
	 * @see NodeParameter(String name, String value)
	 */
	public NodeParameter(String name, String value) throws ValidationException {
		this(name, value, "");
	}

	public NodeParameter(String name) throws ValidationException {
		super(name);
	}

	public void setValue(String value) throws ValidationException {
		setUnsafeValue(value);
		validate(this);
	}

	public void setUnsafeValue(String value) throws ValidationException {
		super.setValue(value);
		if (!isStringValue(value)) {
			setMappedValue(true);
		}
	}

	public boolean isMappedValue() {
		return isMappedValue;
	}

	public void setMappedValue(boolean isMappedValue) {
		this.isMappedValue = isMappedValue;
	}

	public boolean isStringValue() {
		return NodeParameter.isStringValue(getValue());
	}

	@Override
	public NodeParameter copy() throws ValidationException {
		NodeParameter copy = (NodeParameter) copyTo(new NodeParameter(getName(), getValue(), getDescription()));
		copy.setMappedValue(isMappedValue());
		return copy;
	}

}
