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

import org.junit.Test;
import org.simpleframework.xml.Attribute;
import org.w3c.dom.Document;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.persistence.Metadata;

public class SerializationUtilTest {

	@Test
	public void verifySerializationCycle() throws SlipStreamClientException {

		TestClass test = new TestClass("name", "resourceUrl");

		String asString = SerializationUtil.toXmlString(test);
		TestClass recovered = (TestClass) SerializationUtil.fromXml(asString,
				TestClass.class);

		assertEquals(test.getName(), recovered.getName());
		assertEquals(test.getResourceUri(), recovered.getResourceUri());
	}

	@Test
	public void verifySerializationCycleWithDocument()
			throws SlipStreamClientException {

		TestClass test = new TestClass("name", "resourceUrl");

		Document asDocument = SerializationUtil.toXmlDocument(test);
		String asString = SerializationUtil.documentToString(asDocument);
		TestClass recovered = (TestClass) SerializationUtil.fromXml(asString,
				TestClass.class);

		assertEquals(test.getName(), recovered.getName());
		assertEquals(test.getResourceUri(), recovered.getResourceUri());
	}

	@SuppressWarnings("serial")
	public static class TestClass extends Metadata {

		@Attribute
		private String name;

		@Attribute
		private String resourceUrl;

		public TestClass() {

		}

		public TestClass(String name, String resourceUrl) {
			this.name = name;
			this.resourceUrl = resourceUrl;
		}

		@Override
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String getResourceUri() {
			return resourceUrl;
		}

		public void setResourceUrl(String resourceUrl) {
			this.resourceUrl = resourceUrl;
		}

		@Override
		public void remove() {
		}

	}

}
