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

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.MapKey;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import org.simpleframework.xml.ElementMap;

import com.sixsq.slipstream.exceptions.ValidationException;

// The mapping between a parameterized class and its associated 
// parameter type must be given here.
//
// Type S = the subclass of Parameterized and
// Type T = the corresponding subclass of Parameter
// 
// For example, use <User, UserParameter> for the user parameter
// mapping.
//
@MappedSuperclass
@SuppressWarnings("serial")
public abstract class Parameterized<S, T extends Parameter<S>> extends Metadata {

	@ElementMap(name = "parameters", required = false, data = true, valueType = Parameter.class)
	@MapKey(name = "name")
	@OneToMany(mappedBy = "container", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	protected Map<String, T> parameters = new HashMap<String, T>();

	/**
	 * Do not add parameters to the map directly. Instead use setParameter
	 * method
	 */
	public Map<String, T> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, T> parameters) {
		this.parameters = parameters;
	}

	public T getParameter(String name) {
		return getParameters().get(name);
	}

	public String getParameterValue(String name, String defaultValue)
			throws ValidationException {
		T parameter = getParameter(name);
		return parameter == null ? defaultValue : parameter
				.getValue(defaultValue);
	}

	public void setParameter(T parameter) throws ValidationException {
		T target;
		if (parameters.containsKey(parameter.getName())) {
			target = parameters.get(parameter.getName());
			target.setValue(parameter.getValue());
		} else {
			target = parameter;
			parameters.put(target.getName(), target);
		}
		setContainer(target);
	}

	// This method is necessary because setting the container directly here
	// cannot guarantee that the types are correct.
	public abstract void setContainer(T parameter);

	public Parameter<S> getParameter(String name, String category) {
		Parameter<S> parameter = getParameter(name);
		if (parameter != null && parameter.getCategory().equals(category)) {
			return parameter;
		} else {
			return null;
		}
	}

	public Map<String, Parameter<S>> getParameters(String category) {
		Map<String, Parameter<S>> filteredParameters = new HashMap<String, Parameter<S>>();
		boolean isDummy;
		for (Parameter<S> parameter : getParameters().values()) {
			isDummy = RuntimeParameter.NAME_DUMMY.equals(parameter.getName());
			if (parameter.getCategory().equals(category) && !isDummy) {
				filteredParameters.put(parameter.getName(), parameter);
			}
		}

		return filteredParameters;
	}

	public Collection<T> getParameterList() {
		return getParameters().values();
	}

	protected void validateParameters() throws ValidationException {
		for (Entry<String, T> p : getParameters().entrySet()) {
			p.getValue().validateValue();
		}
	}

	@SuppressWarnings("unchecked")
	protected Parameterized<S, T> copyTo(Parameterized<S, T> copy)
			throws ValidationException {
		copy = (Parameterized<S, T>) super.copyTo(copy);
		return copyParametersTo(copy);
	}

	protected Parameterized<S, T> copyParametersTo(
			Parameterized<S, T> parameterized) throws ValidationException {

		for (T p : getParameters().values()) {
			@SuppressWarnings("unchecked")
			T copy = (T) p.copy();
			parameterized.setParameter(copy);
		}

		return parameterized;
	}

	@SuppressWarnings("unchecked")
	public void postDeserialization() {
		super.postDeserialization();
		// Assign containers inside parameters
		for(Entry<String, T> p : getParameters().entrySet()) {
			p.getValue().setContainer((S) this);
		}
	}
}
