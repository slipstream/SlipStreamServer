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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sixsq.slipstream.exceptions.SlipStreamInternalException;

/**
 * Simple utilities to allow static pages/templates to be loaded from the
 * classpath.
 */
public class ResourceUtils {

	private final static Class<? extends Object> myClass = ResourceUtils.class;

	private ResourceUtils() {
	}

	/**
	 * Read in a named resource and return the contents of the resource in a
	 * string.
	 * 
	 * @param resourceName
	 * @return contents of the resource
	 */
	public static String getResource(String resourceName) {
		return getResourceAsString(myClass, resourceName);
	}

	/**
	 * Read in a named resource and return the contents of the resource in a
	 * string.
	 * 
	 * @param resourceName
	 * @return contents of the resource
	 */
	public static String getResourceAsString(Class<?> myClass,
			String resourceName) {

		String result = null;

		try {
			InputStream is = myClass.getResourceAsStream(resourceName);
			CharArrayWriter writer = new CharArrayWriter();
			try {
				if (is != null) {
					for (int c = is.read(); c >= 0; c = is.read()) {
						writer.write(c);
					}
					result = writer.toString();
				} else {
					throw new SlipStreamInternalException(resourceName
							+ " could not be found");
				}
			} finally {
				writer.close();
				if (is != null) {
					is.close();
				}
			}
		} catch (IOException ioe) {
			throw new SlipStreamInternalException(ioe);
		}

		return result;
	}

	public static Document getResourceAsDocument(Class<?> myClass,
			String resourceName) {

		InputStream is = myClass.getResourceAsStream(resourceName);

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			InputSource source = new InputSource(is);

			return db.parse(source);

		} catch (ParserConfigurationException e) {
			throw new SlipStreamInternalException(e);
		} catch (SAXException e) {
			throw new SlipStreamInternalException(e);
		} catch (IOException e) {
			throw new SlipStreamInternalException(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new SlipStreamInternalException(e);
				}
			}
		}

	}

}
