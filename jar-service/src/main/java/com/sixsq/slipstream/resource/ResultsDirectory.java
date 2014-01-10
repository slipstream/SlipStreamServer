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

import static org.restlet.data.MediaType.APPLICATION_JSON;
import static org.restlet.data.MediaType.APPLICATION_XML;

import java.util.ArrayList;
import java.util.List;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.ReferenceList;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Directory;

import com.google.gson.Gson;
import com.sixsq.slipstream.util.SerializationUtil;

public class ResultsDirectory extends Directory {

    public ResultsDirectory(Context context, Reference rootLocalReference) {
        super(context, rootLocalReference);
        setDefaults();
    }

    public ResultsDirectory(Context context, String rootUri) {
        super(context, rootUri);
        setDefaults();
    }

    private void setDefaults() {
        setModifiable(true);
        setListingAllowed(true);
        setNegotiatingContent(true);
    }

    public Representation getIndexRepresentation(Variant variant,
            ReferenceList indexContent) {

        MediaType mediaType = variant.getMediaType();

        if (APPLICATION_JSON.isCompatible(mediaType)) {
            return getJsonRepresentation(indexContent);
        } else if (APPLICATION_XML.isCompatible(mediaType)) {
            return getXmlRepresentation(indexContent);
        } else {
            return super.getIndexRepresentation(variant, indexContent);
        }
    }

    public List<Variant> getIndexVariants(ReferenceList indexContent) {

        List<Variant> variants = new ArrayList<Variant>();
        variants.addAll(super.getIndexVariants(indexContent));

        variants.add(new Variant(APPLICATION_JSON));
        variants.add(new Variant(APPLICATION_XML));

        return variants;
    }

    public Representation getJsonRepresentation(ReferenceList indexContent) {

        FilePropertiesList data = new FilePropertiesList(indexContent);

        Gson gson = new Gson();
        String result = gson.toJson(data);

        return new StringRepresentation(result, APPLICATION_JSON);
    }

    public Representation getXmlRepresentation(ReferenceList indexContent) {

        FilePropertiesList data = new FilePropertiesList(indexContent);

        String result = SerializationUtil.toXmlString(data);

        return new StringRepresentation(result, APPLICATION_XML);
    }

}
