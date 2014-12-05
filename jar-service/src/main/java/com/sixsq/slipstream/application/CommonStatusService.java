package com.sixsq.slipstream.application;

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

import static org.restlet.data.MediaType.APPLICATION_JSON;
import static org.restlet.data.MediaType.APPLICATION_XHTML;
import static org.restlet.data.MediaType.APPLICATION_XML;
import static org.restlet.data.MediaType.TEXT_HTML;
import static org.restlet.data.MediaType.TEXT_PLAIN;

import java.util.List;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.security.Verifier;
import org.restlet.service.StatusService;
import org.w3c.dom.Document;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.ConfigurationUtil;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.ResourceUriUtil;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.XmlUtil;

public class CommonStatusService extends StatusService {

	@Override
	public Representation getRepresentation(Status status, Request request,
			Response response) {

		try {
			reloadParameters();
		} catch (ConfigurationException e) {
			Util.throwConfigurationException(e);
		} catch (ValidationException e) {
			Util.throwClientValidationError(e.getMessage());
		}

		Representation representation = null;

		User user = null;
		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		if (CookieUtils.verifyAuthnCookie(cookie) == Verifier.RESULT_VALID) {
			String username = CookieUtils.getCookieUsername(cookie);
			try {
				user = User.loadByName(username);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String baseUrlSlash = ResourceUriUtil.getBaseUrlSlash(request);

		Configuration configuration = ConfigurationUtil
				.getConfigurationFromRequest(request);

		ClientInfo clientInfo = request.getClientInfo();
		List<Preference<MediaType>> mediaTypes = clientInfo
				.getAcceptedMediaTypes();

		String error = statusToString(status);

		for (Preference<MediaType> preference : mediaTypes) {

			MediaType desiredMediaType = preference.getMetadata();

			if (TEXT_HTML.isCompatible(desiredMediaType)) {

				return toXhtml(status, response, user, baseUrlSlash,
						configuration.version);

			} else if (APPLICATION_XHTML.isCompatible(desiredMediaType)) {

				return toXhtml(status, response, user, baseUrlSlash,
						configuration.version);

			} else if (APPLICATION_JSON.isCompatible(desiredMediaType)) {

				return toJson(status);

			} else if (TEXT_PLAIN.isCompatible(desiredMediaType)) {

				return toTxt(error);

			} else if (APPLICATION_XML.isCompatible(desiredMediaType)) {

				return toXml(status, error);

			}
		}

		return representation;
	}

	private Representation toXhtml(Status status, Response response, User user,
			String baseUrlSlash, String version) {

		String metadata = "";

		if (user != null) {
			Document doc = SerializationUtil.toXmlDocument(user);
			XmlUtil.addUser(doc, user);
			metadata = SerializationUtil.documentToString(doc);
		}

		return new StringRepresentation(HtmlUtil.toHtmlError(metadata,
				status.getDescription(), status.getCode()), MediaType.TEXT_HTML);
	}

	private Representation toJson(Status status) {

		StringBuilder json = new StringBuilder();

		json.append("{\n");
		json.append("   \"error\": \"" + status.getCode() + "\",\n");
		json.append("   \"reason\": \"" + status.getReasonPhrase() + "\",\n");
		json.append("   \"detail\": \"" + status.getDescription() + "\"\n");
		json.append("}\n");

		Representation representation = new StringRepresentation(
				json.toString());
		representation.setMediaType(APPLICATION_JSON);

		return representation;
	}

	private Representation toTxt(String error) {
		Representation representation;
		representation = new StringRepresentation(error);
		representation.setMediaType(TEXT_PLAIN);
		return representation;
	}

	private Representation toXml(Status status, String error) {
		Representation representation;
		representation = new StringRepresentation("<error code=\""
				+ status.getCode() + "\">" + error + "</error>");
		representation.setMediaType(APPLICATION_XML);
		return representation;
	}

	private void reloadParameters() throws ConfigurationException,
			ValidationException {
		Configuration configuration = Configuration.getInstance();

		String key = ServiceConfiguration.RequiredParameters.SLIPSTREAM_SUPPORT_EMAIL
				.getName();
		ServiceConfigurationParameter parameter = configuration.getParameters()
				.getParameter(key);
		String email = parameter.getValue();

		setContactEmail(email);
	}

	private String statusToString(Status status) {

		return "Error: " + status.getDescription() + " (" + status.getCode()
				+ " - " + status.getReasonPhrase() + ")";

	}
}
