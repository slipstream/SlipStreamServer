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

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.user.UserTest;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class XmlUtilTest extends ResourceTestBase {

	@Test
	public void ensureCleanRemovesElements()
			throws ParserConfigurationException, XPathExpressionException {

		Document document = createNewDocument();

		Element root = document.getDocumentElement();

		Element user = document.createElement("User");
		root.appendChild(user);

		Element services = document.createElement("services");
		root.appendChild(services);

		Element breadcrumbs = document.createElement("breadcrumbs");
		root.appendChild(breadcrumbs);

		assertEquals("1", runXpath("count(/*/User)", document));
		assertEquals("1", runXpath("count(/*/services)", document));
		assertEquals("1", runXpath("count(/*/breadcrumbs)", document));

		XmlUtil.cleanDocumentForDeserialization(document);

		assertEquals("0", runXpath("count(/*/User)", document));
		assertEquals("0", runXpath("count(/*/services)", document));
		assertEquals("0", runXpath("count(/*/breadcrumbs)", document));

	}

	@Test
	public void ensureServicesAdded() throws ParserConfigurationException,
			XPathExpressionException {

		Document document = createNewDocument();

		XmlUtil.addServices(document);

		assertEquals("1", runXpath("count(/*/services)", document));
		assertEquals("2", runXpath("count(/*/services/service)", document));

	}

	@Test
	public void ensureUserInfoAdded() throws ParserConfigurationException,
			XPathExpressionException, ConfigurationException,
			ValidationException {

		Document document = createNewDocument();

		User user1 = UserTest.createUser("test1");
		user1.setSuper(true);

		User user2 = UserTest.createUser("test2");
		user2.setSuper(false);

		XmlUtil.addUser(document, user1);

		String xml = SerializationUtil.documentToString(document);

		assertEquals(user1.getName(), runXpath("/*/User/@name", document));
		assertEquals(user1.getId(),
				runXpath("/*/User/@id", document));
		assertEquals("true", runXpath("/*/User/@issuper", document));

		XmlUtil.addUser(document, user2);

		xml = SerializationUtil.documentToString(document);

		assertEquals(user2.getName(), runXpath("/*/User/@name", document));
		assertEquals(user2.getId(),
				runXpath("/*/User/@id", document));
		assertEquals("", runXpath("/*/User/@isSuper", document));

	}

	@Test
	public void runsAreRemovedDuringDenormalization () throws IOException {
		String originalXML = IOUtils.toString(getClass().getResourceAsStream("/image.xml"));
		Assert.assertTrue(originalXML.contains("</runs>"));

		String denormalizedXML = XmlUtil.denormalize(originalXML);
		Assert.assertFalse(denormalizedXML.contains("</runs>"));
	}

	private String runXpath(String xpath, Document document)
			throws XPathExpressionException {

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xp = xpf.newXPath();
		XPathExpression xpe = xp.compile(xpath);
		return xpe.evaluate(document);
	}

	private Document createNewDocument() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.newDocument();
		Element element = document.createElement("root");
		document.appendChild(element);
		return document;
	}

}
