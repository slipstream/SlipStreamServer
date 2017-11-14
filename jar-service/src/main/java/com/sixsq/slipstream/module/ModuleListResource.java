package com.sixsq.slipstream.module;

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

import java.util.ArrayList;

import org.json.JSONObject;
import org.json.XML;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.module.ModuleView.ModuleViewList;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.ResourceUriUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class ModuleListResource extends BaseResource {

	private String resourceUri = null;

	protected String getResourceUri() {
		return resourceUri;
	}

	@Override
	public void initialize() throws ResourceException {

		resourceUri = ResourceUriUtil.extractResourceUri(getRequest());

		if (resourceUri == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
	}

	private String toXmlString() {
		ModuleViewList moduleViewList = retrieveFilteredModuleViewList();

		return SerializationUtil.toXmlString(moduleViewList);
	}

	@Get("xml")
	public Representation toXml() {
		return new StringRepresentation(toXmlString(), MediaType.APPLICATION_XML);
	}

	@Get("json")
	public Representation toJson() {
		String xml = toXmlString();
		JSONObject obj = XML.toJSONObject(xml);
		return new StringRepresentation(obj.toString(), MediaType.APPLICATION_JSON);
	}

	@Get("html")
	public Representation toHtml() {

		ModuleViewList moduleViewList = retrieveFilteredModuleViewList();

		return new StringRepresentation(HtmlUtil.toHtml(moduleViewList,
				getPageRepresentation(), getUser(), getRequest()),
				MediaType.TEXT_HTML);
	}

	protected String getPageRepresentation() {
		return "modules";
	}

	protected ModuleViewList retrieveFilteredModuleViewList() {

		ModuleViewList modules = new ModuleViewList(Module.viewList(resourceUri));
		return filterAuthz(modules);
	}

	protected ModuleViewList filterAuthz(ModuleViewList moduleViewList) {
		ModuleViewList filtered = new ModuleViewList(
				new ArrayList<ModuleView>());
		for (ModuleView view : moduleViewList.getList()) {
			if (view.getAuthz().canGet(getUser())) {
				filtered.getList().add(view);
			}
		}
		return filtered;
	}

}
