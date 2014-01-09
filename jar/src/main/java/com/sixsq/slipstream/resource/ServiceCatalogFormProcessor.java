package com.sixsq.slipstream.resource;

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

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceCatalog;
import com.sixsq.slipstream.persistence.ServiceCatalogParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.user.FormProcessor;

public class ServiceCatalogFormProcessor extends
		FormProcessor<ServiceCatalog, ServiceCatalogParameter> {

	private String cloud;
	
	public ServiceCatalogFormProcessor(User user, String cloud) {
		super(user);
		this.cloud = cloud;
	}

	@Override
	protected void parseForm() throws ValidationException, NotFoundException {
		super.parseForm();
		setParametrized(getOrCreateParameterized(cloud));
	}

	@Override
	protected ServiceCatalog getOrCreateParameterized(String cloud)
			throws ValidationException, NotFoundException {
		return new ServiceCatalog(cloud);
	}

	@Override
	protected ServiceCatalogParameter createParameter(String name,
			String value, String description) throws SlipStreamClientException {
		return new ServiceCatalogParameter(name, value, description);
	}

	@Override
	protected boolean shouldProcess(String paramName) {
		return partOfThisCloud(paramName);
	}

	private boolean partOfThisCloud(String paramName) {
		return paramName.startsWith(cloud + '.');
	}

}
