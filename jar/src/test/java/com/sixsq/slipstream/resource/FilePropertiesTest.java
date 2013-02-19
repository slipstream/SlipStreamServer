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
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.regex.Matcher;

import org.junit.Test;
import org.restlet.data.Reference;

public class FilePropertiesTest {

    @Test
    public void checkReportRegex() {

        for (Map.Entry<String, Map<String, String>> entry : TestData.DATA
                .entrySet()) {
            String f = entry.getKey();
            Map<String, String> p = entry.getValue();

            Matcher m = FileProperties.REPORT_FILENAME.matcher(f);
            assertTrue(m.matches());
            assertEquals(3, m.groupCount());
            assertEquals(p.get("name"), m.group(0));
            assertEquals(p.get("node"), m.group(1));
            assertEquals(p.get("date"), m.group(2));
            assertEquals(p.get("type"), m.group(3));
        }
    }

    @Test
    public void checkReportReferencePropertiesFromIndexContent() {

        for (Reference r : TestData.getIndexContent()) {
            String key = r.getLastSegment();
            Map<String, String> p = TestData.DATA.get(key);

            FileProperties properties = new FileProperties(r);

            assertEquals(r.toString(), properties.uri);
            assertEquals(p.get("name"), properties.name);
            assertEquals(p.get("node"), properties.node);
            assertEquals(p.get("date"), properties.date);
            assertEquals(p.get("type"), properties.type);

        }
    }
}
