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

import java.util.List;

import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.*;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.module.ModuleVersionView.ModuleVersionViewList;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.resource.BaseResource;

public class ModuleVersionListResource extends BaseResource {

	private String resourceUri = null;

	@Override
	public void initialize() throws ResourceException {

		resourceUri = ResourceUriUtil.extractResourceUri(getRequest());

		if (resourceUri == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
	}

	@Override
	protected void authorize() {

		Module m = Module.loadLatest(resourceUri);

		if (getUser().isSuper()) {
			return;
		}

		if (m == null) {
			throwClientForbiddenError("Module doesn't exist. It may have been deleted");
		}else if (m.getAuthz().canGet(getUser())) {
			return;

		}

		throwClientForbiddenError("Not allowed to access: " + resourceUri);
	}

	@Get("txt")
	public Representation toTxt() {

		String viewList = serialized(Module.viewListAllVersions(resourceUri));
		return new StringRepresentation(viewList);
	}

	@Get("xml")
	public Representation toXml() {

		ModuleVersionViewList list = new ModuleVersionViewList(
				Module.viewListAllVersions(resourceUri));
		String result = SerializationUtil.toXmlString(list);
		return new StringRepresentation(result, MediaType.APPLICATION_XML);
	}

	@Get("html")
	public Representation toHtml() {

		ModuleVersionViewList list = new ModuleVersionViewList(
				Module.viewListAllVersions(resourceUri));

		return new StringRepresentation(
				HtmlUtil.toHtml(list,
						getPageRepresentation(), getUser(), getRequest()),
				MediaType.TEXT_HTML);
	}

	@Delete
	public void deleteAllVersions() {

		List<ModuleVersionView> moduleVersions = Module.viewListAllVersions(resourceUri);

		for (ModuleVersionView moduleVersion: moduleVersions) {
			if(getUser().isSuper() || moduleVersion.getAuthz().canDelete(getUser())) {
				Module module = Module.load(moduleVersion.resourceUri);
				module.setDeleted(true);
				module.store(false);
			}
		}

		Module latest = Module.loadLatest(resourceUri);
		try {
			if (latest == null) {
				String redirectUri = ModuleUriUtil.extractParentUriFromResourceUri(resourceUri);
				redirectTo(ModuleUriUtil.extractParentUriFromResourceUri(redirectUri));
			} else {
				redirectTo(latest.getResourceUri());
			}
		} catch (ValidationException e) {
			throwClientConflicError(e.getMessage());
		}

		postEventDeleted(resourceUri);
	}

	private void redirectTo(String resourceUri) throws ValidationException {
		String absolutePath = RequestUtil.constructAbsolutePath(getRequest(), "/" + resourceUri);
		getResponse().setLocationRef(absolutePath);
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	private void postEventDeleted(String moduleUri) {
		Event.postEvent(moduleUri, Event.Severity.high, "All versions deleted by '" + getUser().getName() + "'", getUser().getName(), Event.EventType.action);
	}

	private String serialized(List<ModuleVersionView> viewList) {
		ModuleVersionViewList moduleViewList = new ModuleVersionViewList(
				viewList);
		return SerializationUtil.toXmlString(moduleViewList);
	}

	protected String getPageRepresentation() {
		return "versions";
	}

}
