package com.sixsq.slipstream.staticcontent;

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

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.representation.Representation;
import org.restlet.routing.Filter;
import org.w3c.dom.Document;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.ResourceUtils;
import com.sixsq.slipstream.util.XmlUtil;

/**
 * Represents essentially static content but with the standard SlipStream look
 * and feel applied.
 * 
 * Note: this file must be in the same corresponding hierarchical directory as
 *		 the static .xml files in the resource/ directory.
 */
public class StaticContentDecorator extends Filter {

	private final Document source;

	public StaticContentDecorator(String name) {
		source = ResourceUtils.getResourceAsDocument(
				StaticContentDecorator.class, name + ".xml");
	}

	@Override
	public int doHandle(Request request, Response response) {
		
		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		User user = CookieUtils.getCookieUser(cookie);

		Configuration configuration = RequestUtil
				.getConfigurationFromRequest(request);

		String resourceUrl = RequestUtil.extractResourceUri(request);

		String baseUrlSlash = RequestUtil.getBaseUrlSlash(request);

		// Shallow copy! No changes should be made to the existing content
		// (i.e. the raw-content element)!
		Document template = (Document) source.cloneNode(true);

		XmlUtil.addUser(template, user);
		XmlUtil.addBreadcrumbs(template, "", resourceUrl);

		Source source = new DOMSource(template);
		Representation representation = HtmlUtil.sourceToHtmlRepresentation(
				source, baseUrlSlash, resourceUrl, configuration.version,
				"raw-content.xsl");

		response.setEntity(representation);

		return CONTINUE;
	}

}
