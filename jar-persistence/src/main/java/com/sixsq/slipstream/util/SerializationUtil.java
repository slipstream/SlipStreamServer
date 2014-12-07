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
import java.io.Serializable;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import com.sixsq.slipstream.persistence.Metadata;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class SerializationUtil {

	public static Serializable fromXml(String contents,
			Class<? extends Serializable> resultClass)
			throws SlipStreamClientException {

		try {
			Serializer serializer = new Persister();
			return serializer.read(resultClass, contents);
		} catch (Exception e) {
			throw new SlipStreamClientException("cannot deserialize object, with detail: " + e.getMessage(), e);
		}

	}

	public static String toXmlString(Object object) {

		CharArrayWriter writer = new CharArrayWriter();
		try {

			Serializer serializer = new Persister();
			serializer.write(object, writer);
			return writer.toString();

		} catch (Exception e) {
			throw new SlipStreamInternalException(
					"cannot serialize object to string, with detail: "
							+ e.getMessage(), e);
		} finally {
			writer.close();
		}

	}

	public static Document toXmlDocument(Object object) {

		String serialization = toXmlString(object);

		StringReader reader = new StringReader(serialization);

		try {

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(new InputSource(reader));

		} catch (ParserConfigurationException e) {
			throw new SlipStreamInternalException(
					"error converting to XML document", e);
		} catch (SAXException e) {
			throw new SlipStreamInternalException(
					"error converting to XML document", e);
		} catch (IOException e) {
			throw new SlipStreamInternalException(
					"error converting to XML document", e);

		} finally {
			reader.close();
		}

	}

	// This method should really not need to be used outside of this class.
	// It is marked public only to allow testing.
	public static String documentToString(Document document) {

		CharArrayWriter writer = new CharArrayWriter();

		try {

			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(writer);

			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.transform(source, result);

			return writer.toString();

		} catch (TransformerException e) {
			throw new SlipStreamInternalException(
					"cannot convert document to string", e);
		} finally {
			writer.close();
		}

	}

	public static String extractCategoryFromXmlEntity(String xml)
			throws SlipStreamClientException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document document = null;
		try {
			db = dbf.newDocumentBuilder();
			StringReader reader = new StringReader(xml);
			document = db.parse(new InputSource(reader));
		} catch (ParserConfigurationException e) {
			throw new SlipStreamClientException(e.getMessage());
		} catch (SAXException e) {
			throw new SlipStreamClientException(e.getMessage());
		} catch (IOException e) {
			throw new SlipStreamClientException(e.getMessage());
		}

		return document.getDocumentElement().getAttributes()
				.getNamedItem("category").getNodeValue();
	}

	public static String extractCategoryFromJsonEntity(String json)
			throws SlipStreamClientException {

		JSONObject obj = new JSONObject(json);
		String category = null;
		try {
			category = obj.getString("category");
		} catch (JSONException e) {
			throw new SlipStreamClientException(e.getMessage());
		}
		return category;

	}

	public static Serializable fromJson(String contents,
			Class<? extends Serializable> resultClass)
			throws SlipStreamClientException {

		try {
			return new JSONDeserializer<Metadata>().use(null, resultClass)
					.deserialize(contents);
		} catch (Exception e) {
			throw new SlipStreamClientException("cannot deserialize object", e);
		}

	}

	public static String toJsonString(Serializable obj) {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.exclude("*.class").deepSerialize(obj);
	}
}
