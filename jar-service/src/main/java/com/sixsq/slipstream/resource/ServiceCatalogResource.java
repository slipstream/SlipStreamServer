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

import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotImplementedException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceCatalog;
import com.sixsq.slipstream.persistence.ServiceCatalogParameter;
import com.sixsq.slipstream.util.SerializationUtil;

public class ServiceCatalogResource extends SimpleResource {

	ServiceCatalog serviceCatalog = null;

	@Override
	public void initialize() throws ResourceException {

		super.initialize();

		try {
			if (!ServiceCatalogsResource.serviceCatalogsEnabled()) {
				throwNotFoundResource();
			}
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		// Service catalogs are loaded from the system configuration
		// Therefore they already exist in the system
		serviceCatalog = ServiceCatalog.load(getResourceUri());
		if (serviceCatalog == null) {
			throwNotFoundResource();
		}
	}

	@Get("xml|html")
	public Representation toXml() {

		String result = null;
		try {
			result = SerializationUtil.toXmlString(retrieveServiceCatalog());
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}
		return new StringRepresentation(result);
	}

	private ServiceCatalog retrieveServiceCatalog() throws ValidationException {
		String cloud = (String) getRequestAttributes().get("cloud");
		return ServiceCatalog.loadByCloud(cloud);
	}

	/**
	 * Only expects single catalog entry
	 */
	@Put("xml")
	public void updateOrCreateFromXml(Representation entity)
			throws ResourceException {

		parseToServiceCatalog();
		serviceCatalog = serviceCatalog.store();

	}

	private ServiceCatalog parseToServiceCatalog() {

		String xml = extractXml();

		ServiceCatalog s = null;
		try {
			s = (ServiceCatalog) SerializationUtil.fromXml(xml,
					ServiceCatalog.class);
		} catch (SlipStreamClientException e) {
			throwClientBadRequest("Invalid xml service catalog: "
					+ e.getMessage());
		}

		serviceCatalog.clearParameters();

		for (ServiceCatalogParameter p : s.getParameters().values()) {
			try {
				// Don't trust the provided info
				p.setMandatory(false);
				p.setReadonly(true);
				serviceCatalog.setParameter(p);
			} catch (ValidationException e) {
				throwClientBadRequest(e.getMessage());
			}
		}

		try {
			serviceCatalog.populateDefinedParameters();
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		return s;
	}

	private String extractXml() {
		return getRequest().getEntityAsText();
	}

	@Override
	protected String getPageRepresentation() {
		throw new NotImplementedException();
	}

}
