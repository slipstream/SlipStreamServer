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

import org.restlet.Request;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.dashboard.Dashboard;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.module.ModuleListResourceBase;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class DashboardResource extends ModuleListResourceBase {

	private Configuration configuration = null;

	private User user = null;

	private String resourceUri = null;

	@Override
	public void doInit() throws ResourceException {

		Request request = getRequest();

		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		String username = CookieUtils.getCookieUsername(cookie);

		user = User.loadByName(username);

		configuration = RequestUtil.getConfigurationFromRequest(request);
		if (configuration == null) {
			throw new SlipStreamInternalException("configuration is null");
		}

		resourceUri = RequestUtil.extractResourceUri(getRequest());

		if (resourceUri == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		}
	}

	private User extractUserFromQuery() {
		String username = (String) getRequest().getAttributes().get("user");
		return User.loadByName(username);
	}

	@Get("xml")
	public Representation toXml() {

		String metadata = SerializationUtil.toXmlString(computeDashboard());
		return new StringRepresentation(metadata, MediaType.TEXT_XML);

	}

	@Get("html")
	public Representation toHtml() {

		String html = HtmlUtil.toHtml(computeDashboard(),
				"dashboard", user);
		
		return new StringRepresentation(html, MediaType.TEXT_HTML);
	}

	private Dashboard computeDashboard() {
	
		User user = this.user;
	
		if(this.user.isSuper()) {
			user = extractUserFromQuery();
			user = (user == null) ? this.user : user;
		}
		
		Dashboard dashboard = new Dashboard();
		try {
			dashboard.populate(user);
		} catch (SlipStreamClientException e) {
			//throw(new ResourceException(Status.CLIENT_ERROR_CONFLICT, e.getMessage()));
		} catch (SlipStreamException e) {
			//throw(new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage()));
		}
	
		return dashboard;
	}

}
