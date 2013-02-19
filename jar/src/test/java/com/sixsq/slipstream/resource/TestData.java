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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.restlet.data.ReferenceList;

public class TestData {

    public static final Map<String, Map<String, String>> DATA;
    static {

        Map<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();

        Map<String, String> p = new HashMap<String, String>();
        p.put("name", "apache1.1_report_2012-01-12T073003Z.tgz");
        p.put("node", "apache1.1");
        p.put("date", "2012-01-12T073003Z");
        p.put("type", "tgz");

        data.put(p.get("name"), Collections.unmodifiableMap(p));

        p = new HashMap<String, String>();
        p.put("name", "orchestrator_report_2012-01-12T103215Z.tgz");
        p.put("node", "orchestrator");
        p.put("date", "2012-01-12T103215Z");
        p.put("type", "tgz");

        data.put(p.get("name"), Collections.unmodifiableMap(p));

        p = new HashMap<String, String>();
        p.put("name", "testclient1.1_report_2012-01-12T093353Z.tgz");
        p.put("node", "testclient1.1");
        p.put("date", "2012-01-12T093353Z");
        p.put("type", "tgz");

        data.put(p.get("name"), Collections.unmodifiableMap(p));

        DATA = Collections.unmodifiableMap(data);
    }

    private TestData() {

    }

    public static ReferenceList getIndexContent() {

        ReferenceList referenceList = new ReferenceList();

        String baseUrl = "http://example.org/";
        for (String f : DATA.keySet()) {
            referenceList.add(baseUrl + f);
        }

        return referenceList;
    }

}