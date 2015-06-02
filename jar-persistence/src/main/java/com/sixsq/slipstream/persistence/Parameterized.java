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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

//import org.hibernate.annotations.Type;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration.ParameterCategory;

import flexjson.JSON;

@MappedSuperclass
@SuppressWarnings("serial")
public abstract class Parameterized extends Metadata {

	protected Parameterized(String resourceUriType) {
		super(resourceUriType);
	}

	/**
	 * @Transient such that it's not persisted, but we must add @JSON to ensure
	 *            it gets serialized.
	 */
	@Transient
	@JSON
	protected Map<String, Parameter> parameters = new HashMap<String, Parameter>();

	/**
	 * parameters accessors have to be overriden to set the valueType value of
	 * the decorator for xml serialization:
	 * 
	 * @ElementMap(name = "parameters", required = false, valueType = ?)
	 */
	@ElementMap(name = "parameters", required = false)
	protected void setParameters(Map<String, Parameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Do not add parameters to the map directly. Instead use setParameter
	 * method
	 * 
	 * parameters accessors have to be overriden to set the valueType value of
	 * the decorator for xml serialization:
	 * 
	 * @ElementMap(name = "parameters", required = false, valueType = ?)
	 */
	@ElementMap(name = "parameters", required = false)
	public Map<String, Parameter> getParameters() {
		return parameters;
	}

	public Parameter getParameter(String name) {
		return getParameters().get(name);
	}

	public String getParameterValue(String name, String defaultValue) {
		Parameter parameter = getParameter(name);
		return parameter == null ? defaultValue : parameter.getValue(defaultValue);
	}

	public void setParameter(Parameter parameter) {
		validateParameter(parameter);
		parameters.put(parameter.getName(), parameter);
		setContainer(parameter);
	}

	protected void validateParameter(Parameter parameter) {
	}

	public void setContainer(Parameter parameter) {
		parameter.setContainer(this);
	}

	public Parameter getParameter(String name, String category) {
		Parameter parameter = getParameter(name);
		if (parameter != null && parameter.getCategory().equals(category)) {
			return parameter;
		} else {
			return null;
		}
	}

	public Map<String, Parameter> getParameters(String category) {
		Map<String, Parameter> filteredParameters = new ConcurrentHashMap<String, Parameter>();
		for (Parameter parameter : getParameters().values()) {
			String pCategory = parameter.getCategory();
			if (pCategory.equals(category)) {
				filteredParameters.put(parameter.getName(), parameter);
			}
		}

		return filteredParameters;
	}

	@JSON(include = false)
	public Collection<Parameter> getParameterList() {
		return getParameters().values();
	}

	protected void validateParameters() throws ValidationException {
		for (Entry<String, Parameter> p : getParameters().entrySet()) {
			p.getValue().validateValue();
		}
	}

	protected Parameterized copyTo(Parameterized copy) throws ValidationException {
		copy = (Parameterized) super.copyTo(copy);
		return copyParametersTo(copy);
	}

	protected Parameterized copyParametersTo(Parameterized parameterized) throws ValidationException {

		for (Parameter p : getParameters().values()) {
			Parameter copy = (Parameter) p.copy();
			parameterized.setParameter(copy);
		}

		return parameterized;
	}

	public void postDeserialization() {
		super.postDeserialization();
		// Assign containers inside parameters
		for (Entry<String, Parameter> p : getParameters().entrySet()) {
			p.getValue().setContainer(this);
		}
	}

	public boolean parametersContainKey(String key) {
		return (key == null) ? false : getParameters().containsKey(key);

	}

	public Parameter getParameter(String name, ParameterCategory category) {
		Parameter parameter = getParameter(name);
		if (parameter != null && parameter.getCategory().equals(category.name())) {
			return parameter;
		} else {
			return null;
		}
	}

	public Map<String, Parameter> getParameters(ParameterCategory category) {
		Map<String, Parameter> filteredParameters = new HashMap<String, Parameter>();
		for (Parameter parameter : getParameters().values()) {
			if (parameter.getCategory().equals(category.name())) {
				filteredParameters.put(parameter.getName(), parameter);
			}
		}

		return filteredParameters;
	}

	public static Metadata load(String id, Class<? extends Metadata> type) {
		Parameterized meta = (Parameterized) Metadata.load(id, type);
		if(meta != null) {
			for (Parameter p : meta.getParameterList()) {
				Metadata m = meta;
				p.setContainer(m);
			}
		}
		return meta;
	}

}
