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

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.restlet.Request;
import org.restlet.data.Cookie;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.XmlUtil;

public class SimpleRepresentationBaseResource extends ServerResource {

	protected User user = null;

	protected Configuration configuration = null;

	protected ServiceConfiguration cfg = null;

	protected String baseUrlSlash = null;

	protected String resourceUri = null;

	private String message;

	@Override
	protected void doInit() throws ResourceException {

		Request request = getRequest();

		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		user = CookieUtils.getCookieUser(cookie);

		baseUrlSlash = RequestUtil.getBaseUrlSlash(request);

		configuration = RequestUtil.getConfigurationFromRequest(request);

		resourceUri = RequestUtil.extractResourceUri(request);

		cfg = RequestUtil.getServiceConfigurationFromRequest(request);

	}

	protected void setPostResponse() {
		Representation representation = null;
			try {
				representation = getHtmlRepresentation();
			} catch (ParserConfigurationException e) {
				handleError(e);
			} catch (SAXException e) {
				handleError(e);
			} catch (IOException e) {
				handleError(e);
			}
		getResponse().setEntity(representation);

		getResponse().setStatus(Status.SUCCESS_CREATED);
	}

	protected void handleError(Exception e) {
		e.printStackTrace();
		throw(new ResourceException(Status.SERVER_ERROR_INTERNAL, e));
	}

	protected Representation getHtmlRepresentation()
			throws ParserConfigurationException, SAXException, IOException {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		db = dbf.newDocumentBuilder();

		StringReader reader = new StringReader(createRawMessage());
		Document document = db.parse(new InputSource(reader));

		XmlUtil.addUser(document, user);
		XmlUtil.addBreadcrumbs(document, "", resourceUri);
		
		return null;
		// TODO: complete this feature
//		Map<String, Object> parameters = HtmlUtil.createParameters(
//				baseUrlSlash, resourceUri, configuration.version);
//
//		Source source = new DOMSource(document);
//		return HtmlUtil.sourceToHtmlRepresentation(source, "raw-content.xsl",
//				parameters);

	}

	private String createRawMessage() {
		return createRawXmlRepresentation(message, "Registration");
	}

	protected String createRawXmlRepresentation(String message, String title) {
		String raw = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		raw += "<raw-content title=\"" + title + "\">\n";
		raw += "<div class=\"page_head\">\n";
		raw += "<span class=\"pageheadtitle\">" + title + "</span>\n";
		raw += "</div>\n";
		raw += "<div>\n";
		raw += message + "\n";
		raw += "</div>\n";
		raw += "</raw-content>";

		return raw;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
