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

import static org.junit.Assert.assertEquals;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sixsq.slipstream.persistence.User;

public class XmlUtilTest extends ResourceTestBase{

	@Test
	public void ensureCleanRemovesElements()
			throws ParserConfigurationException, XPathExpressionException {

		Document document = createNewDocument();

		Element root = document.getDocumentElement();

		Element user = document.createElement("user");
		root.appendChild(user);

		Element services = document.createElement("services");
		root.appendChild(services);

		Element breadcrumbs = document.createElement("breadcrumbs");
		root.appendChild(breadcrumbs);

		assertEquals("1", runXpath("count(/*/user)", document));
		assertEquals("1", runXpath("count(/*/services)", document));
		assertEquals("1", runXpath("count(/*/breadcrumbs)", document));

		XmlUtil.cleanDocumentForDeserialization(document);

		assertEquals("0", runXpath("count(/*/user)", document));
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
			XPathExpressionException {

		Document document = createNewDocument();

		User user1 = createUser("test1");
		user1.setSuper(true);

		User user2 = createUser("test2");
		user2.setSuper(false);

		XmlUtil.addUser(document, user1);

		assertEquals(user1.getName(), runXpath("/*/user/@username", document));
		assertEquals(user1.getResourceUri(), runXpath("/*/user/@resourceUri",
				document));
		assertEquals("true", runXpath("/*/user/@issuper", document));

		XmlUtil.addUser(document, user2);

		assertEquals(user2.getName(), runXpath("/*/user/@username", document));
		assertEquals(user2.getResourceUri(), runXpath("/*/user/@resourceUri",
				document));
		assertEquals("", runXpath("/*/user/@isSuper", document));

	}

	@Test
	public void ensureBreadcrumbsAdded() throws ParserConfigurationException,
			XPathExpressionException {

		Document document = createNewDocument();

		String pathPrefix = "alpha/beta";
		String path = "gamma/delta/epsilon";

		XmlUtil.addBreadcrumbs(document, pathPrefix, path);

		assertEquals("1", runXpath("count(/*/breadcrumbs)", document));
		assertEquals(pathPrefix, runXpath("/*/breadcrumbs/@path", document));

		assertEquals("3", runXpath("count(/*/breadcrumbs/crumb)", document));

		assertEquals("gamma", runXpath("/*/breadcrumbs/crumb[1]/@name",
				document));
		assertEquals("alpha/beta/gamma", runXpath(
				"/*/breadcrumbs/crumb[1]/@path", document));

		assertEquals("delta", runXpath("/*/breadcrumbs/crumb[2]/@name",
				document));
		assertEquals("alpha/beta/gamma/delta", runXpath(
				"/*/breadcrumbs/crumb[2]/@path", document));

		assertEquals("epsilon", runXpath("/*/breadcrumbs/crumb[3]/@name",
				document));
		assertEquals("alpha/beta/gamma/delta/epsilon", runXpath(
				"/*/breadcrumbs/crumb[3]/@path", document));

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
