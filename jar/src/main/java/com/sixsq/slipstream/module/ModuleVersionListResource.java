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

import org.restlet.Request;
import org.restlet.data.Cookie;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.module.ModuleVersionView.ModuleVersionViewList;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class ModuleVersionListResource extends ModuleListResourceBase {

	private Configuration configuration = null;

	private User user = null;

	private String baseUrlSlash = null;

	private String resourceUri = null;

	@Override
	public void doInit() throws ResourceException {

		Request request = getRequest();

		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		String username = CookieUtils.getCookieUsername(cookie);

		user = User.loadByName(username);

		// FIXME: Add authorization.

		configuration = RequestUtil.getConfigurationFromRequest(request);
		if (configuration == null) {
			throw new SlipStreamInternalException("configuration is null");
		}

		baseUrlSlash = RequestUtil.getBaseUrlSlash(request);

		resourceUri = RequestUtil.extractResourceUri(getRequest());

		if (resourceUri == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
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
		return new StringRepresentation(result);
	}

	@Get("html")
	public Representation toHtml() {

		ModuleVersionViewList list = new ModuleVersionViewList(
				Module.viewListAllVersions(resourceUri));

		return HtmlUtil.transformToHtml(baseUrlSlash, resourceUri,
				configuration.version, getViewStylesheet(), user, list,
				getChooser());
	}

	private String serialized(List<ModuleVersionView> viewList) {
		ModuleVersionViewList moduleViewList = new ModuleVersionViewList(
				viewList);
		return SerializationUtil.toXmlString(moduleViewList);
	}

	protected String getViewStylesheet() {
		return "module-list.xsl";
	}

}
