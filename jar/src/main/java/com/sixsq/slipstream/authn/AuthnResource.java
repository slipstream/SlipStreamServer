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

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.restlet.Request;
import org.restlet.data.Cookie;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.ResourceUtils;
import com.sixsq.slipstream.util.XmlUtil;

public class AuthnResource extends ServerResource {

	protected User user = null;

	protected Configuration configuration = null;

	protected String resourceUri = null;

	protected String baseUrlSlash = null;

	private final String formTemplate;
	
	private boolean isEmbdded = false;

	protected AuthnResource(String templateName) {
		formTemplate = ResourceUtils.getResourceAsString(LogoutResource.class,
				templateName);
	}

	@Override
	public void doInit() throws ResourceException {

		Request request = getRequest();

		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		user = CookieUtils.getCookieUser(cookie);

		configuration = RequestUtil.getConfigurationFromRequest(request);

		resourceUri = RequestUtil.extractResourceUri(request);

		baseUrlSlash = RequestUtil.getBaseUrlSlash(request);
		
		isEmbdded = getRequest().getAttributes().containsKey("embedded");

	}

	@Get("html")
	public Representation toHtml() {

		try {

			// Retrieve the form. Posts must be done back to this URL. The
			// URL may contain query parts that indicate a redirect URL.
			// This is important for returning the user to the requested
			// page after authentication.
			String page = String.format(formTemplate, getRequest().getResourceRef());

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db;
			db = dbf.newDocumentBuilder();

			StringReader reader = new StringReader(page);
			Document document = db.parse(new InputSource(reader));

			XmlUtil.addUser(document, user);
			XmlUtil.addBreadcrumbs(document, "", resourceUri);

			Map<String, Object> parameters = HtmlUtil.createParameters(baseUrlSlash,
					resourceUri, configuration.version);
			
			if(isEmbdded) {
				parameters.put("embedded", "true");
			}

			Source source = new DOMSource(document);
			return HtmlUtil.sourceToHtmlRepresentation(source, "raw-content.xsl", parameters);

		} catch (ParserConfigurationException e) {
			throw new SlipStreamInternalException(e);
		} catch (SAXException e) {
			throw new SlipStreamInternalException(e);
		} catch (IOException e) {
			throw new SlipStreamInternalException(e);
		}

	}

	protected Reference extractRedirectURL(Request request) {

		Reference resourceRef = request.getResourceRef();
		Form queryForm = resourceRef.getQueryAsForm();
		String relativeURL = queryForm.getFirstValue("redirectURL", true);

		Reference baseRefSlash = RequestUtil.getBaseRefSlash(request);

		if (relativeURL != null) {
			return new Reference(baseRefSlash, relativeURL);
		} else {
			return baseRefSlash;
		}
	}

}
