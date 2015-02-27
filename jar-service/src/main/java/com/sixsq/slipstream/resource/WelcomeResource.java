package com.sixsq.slipstream.resource;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceCatalogs;
import com.sixsq.slipstream.persistence.Welcome;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.SerializationUtil;

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

public class WelcomeResource extends SimpleResource {

	@Get("xml|txt")
	public Representation toXml() {

		String result = SerializationUtil.toXmlString(retrieveWelcome());
		return new StringRepresentation(result);
	}

	@Get("html")
	public Representation toHtml() {

		return new StringRepresentation(HtmlUtil.toHtml(retrieveWelcome(), getPageRepresentation(), getUser(),
				getRequest()), MediaType.TEXT_HTML);
	}

	private Welcome retrieveWelcome() {

		Welcome welcome = new Welcome();

		welcome.setModules(retrieveFilteredModuleViewList());

		try {
			if (ServiceCatalogsResource.serviceCatalogsEnabled()) {
				welcome.setServiceCatalogues(retrieveServiceCatalogs());
			}
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		return welcome;
	}

	private ServiceCatalogs retrieveServiceCatalogs() throws ValidationException {

		ServiceCatalogs scs = new ServiceCatalogs();
		scs.loadAll();
		scs.updateForEditing(getUser());

		return scs;

	}

	protected String getPageRepresentation() {
		return "welcome";
	}

}
