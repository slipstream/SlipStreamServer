package com.sixsq.slipstream.util;

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

import static org.restlet.data.MediaType.TEXT_HTML;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.User;

public class HtmlUtil {

	private static final String SLIPSTREAM_VERSION_PARAMETER_NAME = "version";
	private static final String BASE_URL = "baseUrl";
	private static final String RESOURCE_PATH = "resourcePath";
	final static private String ERROR_TEMPLATE = "<error code=\"%d\"><![CDATA[%s]]></error>";

	static public Representation representErrorAsHtml(int code, String message,
			User user, String baseUrlSlash, String version)
			throws SlipStreamException {

		try {

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();

			String msg = String.format(ERROR_TEMPLATE, code, message);
			Reader reader = new StringReader(msg);
			InputSource source = new InputSource(reader);
			Document document = builder.parse(source);

			XmlUtil.addUser(document, user);

			Source xmlSource = new DOMSource(document);

			return sourceToHtmlRepresentation(xmlSource, baseUrlSlash, "",
					version, "error.xsl");

		} catch (ParserConfigurationException e) {
			throw new SlipStreamException(e.getMessage(), e);
		} catch (SAXException e) {
			throw new SlipStreamException(e.getMessage(), e);
		} catch (IOException e) {
			throw new SlipStreamException(e.getMessage(), e);
		}
	}

	public static Representation transformToHtml(String baseUrlSlash,
			String resourceUrl, String version, String stylesheet, User user,
			String data) throws SlipStreamException {

		Map<String, Object> parameters = createParameters(baseUrlSlash,
				resourceUrl, version);

		try {
			return transformToHtml(stylesheet, user, data, parameters);
		} catch (SAXException e) {
			throw new SlipStreamException(e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			throw new SlipStreamException(e.getMessage(), e);
		} catch (IOException e) {
			throw new SlipStreamException(e.getMessage(), e);
		}
	}

	public static Representation transformToHtml(String baseUrlSlash,
			String resourceUrl, String version, String stylesheet, User user,
			Object data) {

		Map<String, Object> parameters = createParameters(baseUrlSlash,
				resourceUrl, version);

		return transformToHtml(stylesheet, user, data, parameters);
	}

	public static Representation transformToHtml(String baseUrlSlash,
			String resourceUrl, String version, String stylesheet, User user,
			Object data, Map<String, Object> parameters) {

		Map<String, Object> _parameters = createParameters(baseUrlSlash,
				resourceUrl, version);

		_parameters.putAll(parameters);

		return transformToHtml(stylesheet, user, data, _parameters);
	}

	public static Representation transformToHtml(String baseUrlSlash,
			String resourceUrl, String version, String stylesheet, User user,
			Object data, boolean isChooser) {

		Map<String, Object> parameters = createParameters(baseUrlSlash,
				resourceUrl, version);
		if(isChooser) {
			parameters.put("chooserType", true);
		}

		return transformToHtml(stylesheet, user, data, parameters);

	}

	public static Map<String, Object> createParameters(String baseUrlSlash,
			String resourceUrl, String version) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(BASE_URL, baseUrlSlash);
		parameters.put(RESOURCE_PATH, resourceUrl);
		parameters.put(SLIPSTREAM_VERSION_PARAMETER_NAME, version);
		return parameters;
	}

	public static Representation transformToHtml(String stylesheet, User user,
			Object data, Map<String, Object> parameters) {

		Document document = SerializationUtil.toXmlDocument(data);

		return transformToHtml(stylesheet, user, document, parameters);

	}

	public static Representation transformToHtml(String stylesheet, User user,
			String data, Map<String, Object> parameters) throws SAXException,
			ParserConfigurationException, IOException {

		Document document = XmlUtil.stringToDom(data);

		return transformToHtml(stylesheet, user, document, parameters);

	}

	public static Representation transformToHtml(String stylesheet, User user,
			Document document, Map<String, Object> parameters) {

		String resourceUrl = parameters.get(RESOURCE_PATH).toString();
		XmlUtil.addUser(document, user);
		XmlUtil.addBreadcrumbs(document, "", resourceUrl);

		Source source = new DOMSource(document);

		String version = parameters.get(SLIPSTREAM_VERSION_PARAMETER_NAME)
				.toString();
		return sourceToHtmlRepresentation(source, parameters.get(BASE_URL)
				.toString(), resourceUrl, version, stylesheet, parameters);

	}

	public static Representation sourceToHtmlRepresentation(Source source,
			String baseUrlSlash, String resourceUrl, String version,
			String stylesheet) {

		Map<String, Object> parameters = createParameters(baseUrlSlash,
				resourceUrl, version);

		return sourceToHtmlRepresentation(source, baseUrlSlash, resourceUrl,
				version, stylesheet, parameters);

	}

	public static Representation sourceToHtmlRepresentation(Source source,
			String baseUrlSlash, String resourceUrl, String version,
			String stylesheet, Map<String, Object> parameters) {

		parameters.put("version", version);
		return sourceToHtmlRepresentation(source, stylesheet, parameters);
	}

	public static Representation sourceToHtmlRepresentation(Source source,
			String stylesheet, Map<String, Object> parameters) {

		return new StringRepresentation(XslUtils.transform(source,
				stylesheet, parameters), TEXT_HTML);
	}
}
