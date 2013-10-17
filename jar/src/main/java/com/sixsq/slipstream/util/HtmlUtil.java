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

package com.sixsq.slipstream.util;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;

import slipstream.ui.views.Representation;

import com.sixsq.slipstream.persistence.User;

public class HtmlUtil {

	public static String toHtml(Object metadata, String page, User user) {
		return toHtml(metadata, page, null, user);
	}

	public static String toHtml(Object metadata, String page, String type,
			User user) {
		Document doc = SerializationUtil.toXmlDocument(metadata);

		XmlUtil.addUser(doc, user);

		String xml = SerializationUtil.documentToString(doc);
		try {
			return Representation.toHtml(xml, page, type);
		} catch (IllegalArgumentException ex) {
			throw (new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					"Unknown resource: " + page));
		}
	}

	public static String toHtml(String metadata, String page) {
		return HtmlUtil.toHtml(metadata, page, "");
	}

	public static String toHtml(String metadata, String page, String type) {
		return Representation.toHtml(metadata, page, type);
	}

	public static String toHtmlError(String metadata, String error, int code) {
		return Representation
				.toHtmlError(metadata, error, String.valueOf(code));
	}

}
