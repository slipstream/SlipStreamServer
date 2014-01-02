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
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class ModuleListResource extends BaseResource {

	private String resourceUri = null;

	@Override
	public void doInit() throws ResourceException {

		super.doInit();

		resourceUri = RequestUtil.extractResourceUri(getRequest());

		if (resourceUri == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
	}

	@Get("xml")
	public Representation toXml() {

		ModuleViewList moduleViewList = retrieveFilteredModuleViewList();

		String result = SerializationUtil.toXmlString(moduleViewList);
		return new StringRepresentation(result, MediaType.APPLICATION_XML);
	}

	@Get("html")
	public Representation toHtml() {

		ModuleViewList moduleViewList = retrieveFilteredModuleViewList();

		return new StringRepresentation(HtmlUtil.toHtml(moduleViewList,
				getPageRepresentation(), getTransformationType(), getUser()),
				MediaType.TEXT_HTML);
	}

	protected String getPageRepresentation() {
		return "project";
	}

	protected ModuleViewList retrieveFilteredModuleViewList() {

		ModuleViewList modules = new ModuleViewList(
				Module.viewList(resourceUri));

		if ("/".equals(getRequest().getResourceRef().getPath())) {
			ModuleViewList published = new ModuleViewList(
					Module.viewPublishedList());
			modules.getList().addAll(published.getList());
		}

		return filterAuthz(modules);
	}

	private ModuleViewList filterAuthz(ModuleViewList moduleViewList) {
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
