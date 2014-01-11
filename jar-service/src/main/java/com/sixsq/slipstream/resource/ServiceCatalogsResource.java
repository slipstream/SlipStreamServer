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

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceCatalog;
import com.sixsq.slipstream.persistence.ServiceCatalogs;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class ServiceCatalogsResource extends SimpleResource {

	private ServiceCatalogs sc = new ServiceCatalogs();

	@Override
	public void doInit() throws ResourceException {

		super.doInit();

		try {
			if(!ServiceCatalogs.isEnabled()) {
				throwNotFoundResource();
			}
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}
	}
	
	@Get("xml")
	public Representation toXml() {

		String result = null;
		try {
			result = SerializationUtil.toXmlString(retrieveServiceCatalogs());
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}
		return new StringRepresentation(result);
	}

	@Get("html")
	public Representation toHtml() {

		StringRepresentation result = null;

		try {
			result = new StringRepresentation(HtmlUtil.toHtml(
					retrieveServiceCatalogs(), getPageRepresentation(),
					getTransformationType(), getUser()), MediaType.TEXT_HTML);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}
		return result;
	}

	public ServiceCatalogs retrieveServiceCatalogs() throws ValidationException {

		ServiceCatalogs scs = new ServiceCatalogs();

		for (ServiceCatalog sc : ServiceCatalog.listall()) {
			sc.populateDefinedParameters();
			scs.getList().add(sc);
		}

		return scs;

	}

	@Put("form")
	public void updateFromForm(Representation entity) throws ResourceException {

		checkIsSuper();

		try {
			processEntityAsForm(entity);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		sc.store();

		getResponse().setLocationRef("/service_catalog");
	}

	public void processEntityAsForm(Representation entity)
			throws ResourceException, ConfigurationException,
			ValidationException {

		Form form = (entity == null) ? new Form() : extractFormFromEntity(entity);

		for (ServiceCatalog s : sc.getList()) {

			ServiceCatalogFormProcessor processor = new ServiceCatalogFormProcessor(
					getUser(), s.getCloud());

			try {
				processor.processForm(form);
			} catch (BadlyFormedElementException e) {
				throwClientError(e);
			} catch (SlipStreamClientException e) {
				throwClientError(e);
			}

			ServiceCatalog proposedServiceCatalog = processor.getParametrized();

			sc.update(proposedServiceCatalog);
		}
	}

	protected String getPageRepresentation() {
		return "service_catalog";
	}

}
