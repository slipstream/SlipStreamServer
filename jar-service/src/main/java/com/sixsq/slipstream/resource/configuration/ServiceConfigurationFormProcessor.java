package com.sixsq.slipstream.resource.configuration;

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

import org.restlet.data.Form;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.user.FormProcessor;

public class ServiceConfigurationFormProcessor extends
		FormProcessor<ServiceConfiguration, ServiceConfigurationParameter> {

	public ServiceConfigurationFormProcessor(User user) {
		super(user);
	}

	@Override
	protected void parseForm() throws ValidationException, NotFoundException {
		super.parseForm();
		setParametrized(getOrCreateParameterized(null));
	}

	@Override
	protected ServiceConfiguration getOrCreateParameterized(String name)
			throws ValidationException, NotFoundException {
		return ServiceConfiguration.load();
	}

	@Override
	protected ServiceConfigurationParameter createParameter(String name,
			String value, String description) throws SlipStreamClientException {
		return new ServiceConfigurationParameter(name, value, description);
	}

	@Override
	protected String extractCategory(Form form, String genericPart) {
		return form.getFirstValue(genericPart + "category");
	}

}
