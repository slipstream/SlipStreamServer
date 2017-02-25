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

import java.util.Map;
import java.util.HashMap;

import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;

import slipstream.ui.views.Representation;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.metrics.Metrics;
import com.sixsq.slipstream.metrics.MetricsTimer;
import com.sixsq.slipstream.persistence.User;

/**
 * For unit tests, @see HtmlUtilTest
 *
 */
public class HtmlUtil {

	private static final String USER_KEY = "user";
	private static final String USERNAME_KEY = "username";
	private static final String USER_URI_KEY = "uri";
	private static final String IS_SUPER_KEY = "super?";

	public static String toHtml(Object metadata, String page, User user, Request request) {
		return toHtml(metadata, page, user, RequestUtil.constructOptions(request));
	}

	private static final MetricsTimer toHtmlTimer = Metrics.newTimer(HtmlUtil.class, "toHtml");

	private static String toHtml(Object metadata, String page, User user, Map<String, Object> options) {

		Document doc = SerializationUtil.toXmlDocument(metadata);

		XmlUtil.addUser(doc, user);
		try {
			XmlUtil.addSystemConfiguration(doc);
		} catch (ConfigurationException e) {
			throw (new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage()));
		} catch (ValidationException e) {
			throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage()));
		}

		String xml = SerializationUtil.documentToString(doc);
		try {
			toHtmlTimer.start();
			return Representation.toHtml(xml, page, options);
		} catch (IllegalArgumentException ex) {
			throw (new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Unknown resource: " + page));
		} finally {
			toHtmlTimer.stop();
		}
	}

	public static String toHtmlFromJson(String json, String page, Request request) {
		return toHtmlFromJson(json, page, null, request);
	}

	public static String toHtmlFromJson(String json, String page, User user, Request request) {
		Map<String, Object> options = RequestUtil.constructOptions(request);

		if (user != null) {
			// TODO: Refactor adding asMap() method to User class with all attributes
			//       (currently only name and isSuper are coded for event resource).
			Map<String, Object> userMap = new HashMap<String, Object>();
			userMap.put(USERNAME_KEY, user.getName());
			userMap.put(IS_SUPER_KEY, user.isSuper());
			userMap.put(USER_URI_KEY, user.getResourceUri());
			options.put(USER_KEY, userMap);
		}

		return toHtmlFromJson(json, page, options);
	}

	private static String toHtmlFromJson(String json, String page, Map<String, Object> options) {
		return Representation.toHtml(json, page, options);
	}

	public static String toHtmlError(String metadata, String error, int code, Request request) {
		return toHtmlError(metadata, error, code, RequestUtil.constructOptions(request));
	}

	private static String toHtmlError(String metadata, String error, int code, Map<String, Object> options) {
		return Representation.toHtmlError(metadata, error, String.valueOf(code), options);
	}

}
