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

import java.io.StringReader;
import java.util.HashMap;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.User;

public class XmlUtil {

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
				Element element = document.createElement("user");
				element.setAttribute("name", user.getName());
				element.setAttribute("resourceUri", user.getResourceUri());
				if (user.isSuper()) {
					element.setAttribute("issuper", "true");
				}

				root.appendChild(element);
			}
		}

	}

	public static void addSystemConfiguration(Document root) {
		Document configurationDoc = SerializationUtil
				.toXmlDocument(Configuration.getInstance().getParameters());
		root.getDocumentElement().appendChild(
				root.importNode(configurationDoc.getFirstChild(), true));
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
