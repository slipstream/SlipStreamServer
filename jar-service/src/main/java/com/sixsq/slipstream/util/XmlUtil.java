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

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Metadata;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.User;

public class XmlUtil {

	public static void addSystemConfiguration(Document root)
			throws ConfigurationException, ValidationException {
		Document configurationDoc = SerializationUtil
				.toXmlDocument(Configuration.getInstance().getParameters());
		root.getDocumentElement().appendChild(
				root.importNode(configurationDoc.getFirstChild(), true));
	}

	/**
	 * Method will strip any existing user elements and then add a new one based
	 * on the given user. If the user is null, then no element will be added.
	 * The document may be null, in which case this is a no-op.
	 *
	 * The added element is named "user" with "username", "resourceUri", and
	 * "issuper" attributes.
	 *
	 * Note: The element will be added inside the root element. If there is no
	 * root, then nothing will be added to the document.
	 *
	 * @param document
	 * @param user
	 */
	public static void addUser(Document document, User user) {

		if (document != null) {
			stripNamedElements(document, "user");

			Element root = document.getDocumentElement();

			if (root != null && user != null) {
				Document userDocument = SerializationUtil.toXmlDocument(user);
				Node node = document.importNode(userDocument.getFirstChild(), true);
				root.appendChild(node);
			}
		}

	}

	private static void stripNamedElements(Document document, String name) {
		Element root = document.getDocumentElement();
		if (root != null) {
			NodeList nodes = root.getElementsByTagName(name);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				node.getParentNode().removeChild(node);
			}
		}
	}

	public static Document stringToDom(String xmlSource) throws SAXException,
			ParserConfigurationException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xmlSource)));
	}

	/**
	 * Normalize xml representation of a module, which is natively denormalized
	 * to facilitate xslt (for example) processing
	 *
	 * @param module
	 * @return
	 */
	public static String normalize(Module module) {

		Document document = SerializationUtil.toXmlDocument(module);
		Source source = new DOMSource(document);

		return XslUtils.transform(source, "module-export.xsl",
				new HashMap<String, Object>());
	}

	/**
	 * Denormalize xml representation of a module, which is natively
	 * denormalized to facilitate xslt (for example) processing
	 *
	 * @param imported
	 * @return
	 */
	public static String denormalize(String external) {

		Source source = new StreamSource(new StringReader(external));
		return XslUtils.transform(source, "module-import.xsl",
				new HashMap<String, Object>());
	}

}
