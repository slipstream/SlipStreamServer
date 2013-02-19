package com.sixsq.slipstream.resource;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.restlet.data.MediaType.APPLICATION_JSON;
import static org.restlet.data.MediaType.APPLICATION_XML;
import static org.restlet.data.MediaType.TEXT_HTML;
import static org.restlet.data.MediaType.TEXT_URI_LIST;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.ReferenceList;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

public class ResultsDirectoryTest {

    @Test
    public void checkDefaultOptions() {
        ResultsDirectory resultsDirectory = getTestResultsDirectory();

        assertEquals(true, resultsDirectory.isListingAllowed());
        assertEquals(true, resultsDirectory.isModifiable());
    }

    @Test
    public void checkVariantsList() {

        Variant[] expectedVariants = new Variant[] { new Variant(TEXT_HTML),
                new Variant(TEXT_URI_LIST), new Variant(APPLICATION_JSON),
                new Variant(APPLICATION_XML) };

        ResultsDirectory resultsDirectory = getTestResultsDirectory();

        List<Variant> variants = resultsDirectory
                .getIndexVariants(new ReferenceList());

        for (Variant expectedVariant : expectedVariants) {
            assertTrue("missing variant: " + expectedVariant.toString(),
                    variants.contains(expectedVariant));
        }
    }

    @Test
    public void checkXmlIndex() throws ResourceException, IOException,
            SAXException, ParserConfigurationException {

        ResultsDirectory resultsDirectory = getTestResultsDirectory();

        Representation xml = resultsDirectory.getXmlRepresentation(TestData
                .getIndexContent());

        assertNotNull(xml);

        xml.write(System.err);

        Document doc = isValidXml(xml.getText());

        assertEquals("files", doc.getDocumentElement().getNodeName());

        NodeList nodes = doc.getDocumentElement().getChildNodes();

        int fileElementCount = 0;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ("file".equals(node.getNodeName())) {
                fileElementCount++;
            }
        }
        assertEquals(TestData.DATA.size(), fileElementCount);

    }

    @Test
    public void checkJsonIndex() throws ResourceException, IOException {

        ResultsDirectory resultsDirectory = getTestResultsDirectory();

        Representation json = resultsDirectory.getJsonRepresentation(TestData
                .getIndexContent());

        assertNotNull(json);

        json.write(System.err);
        FilePropertiesList list = isValidJson(json.getText());

        assertNotNull(list);
        assertEquals(TestData.DATA.size(), list.files.size());
    }

    private ResultsDirectory getTestResultsDirectory() {
        Reference folderReference = new Reference("file:///dummy/dir/");
        return new ResultsDirectory(null, folderReference);
    }

    private static Document isValidXml(String xml) throws SAXException,
            IOException, ParserConfigurationException {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        InputSource is = new InputSource(new StringReader(xml));
        return dBuilder.parse(is);
    }

    private static FilePropertiesList isValidJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, FilePropertiesList.class);
    }

}
