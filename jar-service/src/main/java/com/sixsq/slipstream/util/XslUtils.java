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
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.sixsq.slipstream.exceptions.SlipStreamInternalException;

/**
 * Simple utilities to allow static pages/templates to be loaded from the
 * classpath.
 */
public class XslUtils {

	// Must set the resolver for the factory, so that imported stylesheets are
	// looked up relative to this class!
	private static TransformerFactory factory;
	private static boolean reloadStylesheets = true;

	static {
		createFactory();
	}

	private static void createFactory()
			throws TransformerFactoryConfigurationError {
		factory = TransformerFactory.newInstance();
		factory.setURIResolver(new XslResolver(XslUtils.class));
	}

	private XslUtils() {
	}

	/**
	 * Looks up the named stylesheet and returns a tranformer for it. The
	 * stylesheet is read as a resource and should appear in the CLASSPATH at
	 * the same location as this class.
	 * 
	 * @param xslName
	 *            name of the stylesheet to compile
	 * 
	 * @return Transformer configured with the given stylesheet
	 * 
	 */
	public static Transformer getTransformer(String xslName) {

		Transformer transformer = null;

		InputStream is = XslUtils.class.getResourceAsStream(xslName);
		if (is != null) {
			try {
				Source xsltSource = new StreamSource(is);
				transformer = getFactory().newTransformer(xsltSource);
			} catch (TransformerConfigurationException e) {
				throw new SlipStreamInternalException(
						"error compiling stylesheet: " + xslName, e);
			} finally {
				try {
					is.close();
				} catch (IOException ioe) {
					throw new SlipStreamInternalException(
							"error closing stylesheet: " + xslName, ioe);
				}
			}
		} else {
			throw new SlipStreamInternalException("stylesheet " + xslName
					+ " not found");
		}

		return transformer;
	}

	private static TransformerFactory getFactory() {
		if (reloadStylesheets) {
			createFactory();
		}
		return factory;
	}

	private static class XslResolver implements URIResolver {

		private final Class<?> baseClass;

		public XslResolver(Class<?> baseClass) {
			this.baseClass = baseClass;
		}

		public Source resolve(String href, String base) {

			InputStream is = baseClass.getResourceAsStream(href);
			if (is != null) {
				return new StreamSource(is);
			} else {
				return null;
			}

		}
	}

	public static String transform(Source source,
			String stylesheet, Map<String, Object> parameters) {

		CharArrayWriter writer = new CharArrayWriter();
		Result result = new StreamResult(writer);

		Transformer transformer = XslUtils.getTransformer(stylesheet);

		for (Entry<String, Object> parameter : parameters.entrySet()) {
			String value = (parameter.getValue() == null ? "" : parameter
					.getValue().toString());
			transformer.setParameter(parameter.getKey(), value);
		}

		try {
			transformer.transform(source, result);
			return writer.toString();
		} catch (TransformerException e) {
			throw new SlipStreamInternalException(e);
		} finally {
			writer.close();
		}
	}
}
