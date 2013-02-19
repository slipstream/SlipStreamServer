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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.restlet.data.Reference;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "file")
public class FileProperties {

    public static final Pattern REPORT_FILENAME = Pattern
            .compile("(.*)_report_(.*?)\\.(.*)");

    @Element
    public final String uri;

    @Element
    public final String name;

    @Element
    public final String type;

    @Element
    public final String node;

    @Element
    public final String date;

    public FileProperties(Reference r) {

        uri = r.toString();
        name = r.getLastSegment();

        Matcher m = REPORT_FILENAME.matcher(name);

        if (m.matches() && m.groupCount() == 3) {
            node = m.group(1);
            date = m.group(2);
            type = m.group(3);
        } else {
            node = null;
            date = null;
            type = "unknown";
        }
    }

}
