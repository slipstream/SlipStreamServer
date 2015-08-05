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
	public void ensureUserInfoAdded() throws ParserConfigurationException,
			XPathExpressionException, ConfigurationException,
			ValidationException {

		Document document = createNewDocument();

		User user1 = UserTest.createUser("test1");
		user1.setSuper(true);

		User user2 = UserTest.createUser("test2");
		user2.setSuper(false);

		XmlUtil.addUser(document, user1);

		assertEquals(user1.getName(), runXpath("/*/user/@name", document));
		assertEquals(user1.getResourceUri(),
				runXpath("/*/user/@resourceUri", document));
		assertEquals("true", runXpath("/*/user/@issuper", document));

		XmlUtil.addUser(document, user2);

		assertEquals(user2.getName(), runXpath("/*/user/@name", document));
		assertEquals(user2.getResourceUri(),
				runXpath("/*/user/@resourceUri", document));
		assertEquals("", runXpath("/*/user/@isSuper", document));

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
