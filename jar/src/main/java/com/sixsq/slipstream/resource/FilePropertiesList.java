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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.restlet.data.Reference;
import org.restlet.data.ReferenceList;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name = "files")
public class FilePropertiesList {

    @ElementList(inline = true)
    public final List<FileProperties> files;

    public FilePropertiesList(ReferenceList indexContent) {
        List<FileProperties> entries = new ArrayList<FileProperties>();
        for (Reference r : indexContent) {
            entries.add(new FileProperties(r));
        }
        files = Collections.unmodifiableList(entries);
    }
}
