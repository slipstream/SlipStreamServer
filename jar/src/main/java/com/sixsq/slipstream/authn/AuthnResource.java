package com.sixsq.slipstream.authn;

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
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class AuthnResource extends ServerResource {

	protected User user = null;

	protected Configuration configuration = null;

	protected String resourceUri = null;

	protected String baseUrlSlash = null;

	private final String templateName; // name of the page

	protected AuthnResource(String templateName) {
		this.templateName = templateName;
	}

	@Override
	public void doInit() throws ResourceException {

		Request request = getRequest();

		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		user = CookieUtils.getCookieUser(cookie);

		configuration = RequestUtil.getConfigurationFromRequest(request);

		resourceUri = RequestUtil.extractResourceUri(request);

		baseUrlSlash = RequestUtil.getBaseUrlSlash(request);

	}

	@Get("html")
	public Representation toHtml() {

		String metadata = "";
		if (user != null) {
			Document document = SerializationUtil.toXmlDocument(user);
			metadata = SerializationUtil.documentToString(document);
		}
		return new StringRepresentation(HtmlUtil.toHtml(metadata, templateName,
				null, user), MediaType.TEXT_HTML);
	}

	protected Reference extractRedirectURL(Request request) {
		return extractRedirectURL(request, null);
	}

	/**
	 * If the defaultUrl is null and no redirect URL query parameter is
	 * provided, the redirect URL is the base URL.
	 * 
	 * @param request
	 * @param defaultUrl
	 * @return redirectUrl
	 */
	protected Reference extractRedirectURL(Request request, String defaultUrl) {

		Reference resourceRef = request.getResourceRef();
		Form queryForm = resourceRef.getQueryAsForm();
		String relativeURL = queryForm.getFirstValue("redirectURL", true);

		Reference baseRefSlash = RequestUtil.getBaseRefSlash(request);

		Reference redirectUrl = null;
		if (relativeURL != null) {
			redirectUrl = new Reference(baseRefSlash, relativeURL);
		} else if (defaultUrl != null) {
			redirectUrl = new Reference(baseRefSlash, defaultUrl);
		} else {
			redirectUrl = new Reference(baseRefSlash);
		}

		return redirectUrl;
	}

}
