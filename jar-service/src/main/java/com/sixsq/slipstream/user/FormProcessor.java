package com.sixsq.slipstream.user;

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

import java.util.concurrent.ConcurrentHashMap;

import org.restlet.data.Form;

import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterType;
import com.sixsq.slipstream.persistence.Parameterized;
import com.sixsq.slipstream.persistence.User;

public abstract class FormProcessor<S extends Parameterized<S, T>, T extends Parameter<S>> {

	private S parametrized;
	private User user;
	private Form form;

	public static boolean isSet(String value) {
		return Parameter.hasValueSet(value);
	}

	public FormProcessor(User user) {
		this.user = user;
	}

	protected Form getForm() {
		return form;
	}

	public User getUser() {
		return user;
	}

	public S getParametrized() {
		return parametrized;
	}

	protected void setParametrized(S parametrized) {
		this.parametrized = parametrized;
	}

	abstract protected S getOrCreateParameterized(String name)
			throws ValidationException, NotFoundException;

	public void processForm(Form form) throws BadlyFormedElementException,
			SlipStreamClientException {

		this.form = form;

		parseForm();
		parseFormParameters();
		parseAuthz();
		// validate();
	}

	protected void parseAuthz() {
		return;
	}

	protected void parseForm() throws ValidationException, NotFoundException {
		return;
	}

	protected void parseFormParameters() throws BadlyFormedElementException,
			SlipStreamClientException {
		// the params are in the form:
		// - parameter--[id]--name
		// - parameter--[id]--description
		// - parameter--[id]--value
		// ...
		S parametrised = getParametrized();
		if (parametrised != null) {
			parametrised.setParameters(new ConcurrentHashMap<String, T>(
					parametrised.getParameters()));
		}
		for (String paramName : form.getNames().toArray(new String[0])) {

			if (isParameterName(paramName)) {
				processSingleParameter(form, paramName);
			}

		}
	}

	private boolean isParameterName(String paramName) {
		return paramName.startsWith("parameter-")
				&& paramName.endsWith("--name");
	}

	protected void processSingleParameter(Form form, String paramName)
			throws BadlyFormedElementException, SlipStreamClientException {

		String genericPart = getGenericPart(paramName);
		String name = form.getFirstValue(paramName);

		String value = extractValue(form, genericPart);

		if(!shouldProcess(name)) {
			return;
		}
	 	
		boolean exists = (name == null) ? false : getParametrized()
				.parametersContainKey(name);
		if (exists) {
			setExistingParameter(name, value);
		} else {
			setNewParameter(form, genericPart, name, value);
		}

	}

	protected boolean shouldProcess(String paramName) throws ValidationException {
		return true;
	}

	protected void setExistingParameter(String name, String value)
			throws ValidationException {
		T parameter;
		parameter = getParametrized().getParameter(name);
		boolean overwrite = shouldSetValue(parameter, value);

		if (overwrite) {
			parameter.setValue(value);
		}
	}

	protected void setNewParameter(Form form, String genericPart, String name,
			String value) throws SlipStreamClientException, ValidationException {
		T parameter;
		String description = extractDescription(form, genericPart);

		parameter = createParameter(name, value, description);

		parameter.setMandatory(extractMandatory(form, genericPart));
		parameter.setCategory(extractCategory(form, genericPart));
		parameter.setType(extractType(form, genericPart));
		parameter.setValue(value); // once the type is set, set the value again

		if (shouldSetValue(parameter, value)) {
			parametrized.setParameter(parameter);
		}
	}

	private boolean shouldSetValue(T parameter, String value) {
		return !parameter.isReadonly() || user.isSuper();
	}

	protected abstract T createParameter(String name, String value,
			String description) throws SlipStreamClientException;

	protected String getGenericPart(String paramName) {
		String[] parts = paramName.split("--");
		int lastSize = parts[parts.length - 1].length();
		return paramName.substring(0, paramName.length() - lastSize);
	}

	private String extractValue(Form form, String genericPart) {
		return form.getFirstValue(genericPart + "value");
	}

	private String extractDescription(Form form, String genericPart) {
		return form.getFirstValue(genericPart + "description");
	}

	private boolean extractMandatory(Form form, String genericPart) {
		return Boolean.parseBoolean(form.getFirstValue(genericPart
				+ "mandatory"));
	}

	protected String extractCategory(Form form, String genericPart) {
		return form.getFirstValue(genericPart + "category");
	}

	private ParameterType extractType(Form form, String genericPart) {
		return ParameterType.valueOf(form.getFirstValue(genericPart + "type"));
	}

}
