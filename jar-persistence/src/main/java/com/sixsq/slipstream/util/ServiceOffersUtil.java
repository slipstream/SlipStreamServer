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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sixsq.slipstream.exceptions.ValidationException;
import org.restlet.Response;
import org.restlet.resource.ResourceException;

public class ServiceOffersUtil {

    public static String cpuAttributeName = "resource:vcpu";
    public static String ramAttributeName = "resource:ram";
    public static String diskAttributeName = "resource:disk";
    public static String instanceTypeAttributeName = "resource:instanceType";


    public static JsonObject parseJson(String json) {
        return new JsonParser().parse(json).getAsJsonObject();
    }

    public static JsonObject getServiceOffer(String serviceOfferId) {
        return getServiceOffer(serviceOfferId, false);
    }

    public static JsonObject getServiceOffer(String serviceOfferId, boolean throwIfNotFound) {
        Response response = null;
        try {
            response = SscljProxy.get(SscljProxy.BASE_RESOURCE + serviceOfferId, "super ADMIN", true);
        } catch (ResourceException e) {
            if (!throwIfNotFound && e.getStatus().getCode() == 404) {
                return null;
            }
            throw e;
        }
        return parseJson(response.getEntityAsText());
    }

    public static String getServiceOfferAttributeOrNull(JsonObject serviceOffer, String serviceOfferAttributeName) {
        try {
            return getServiceOfferAttribute(serviceOffer, serviceOfferAttributeName);
        } catch (ValidationException ignored){
            return null;
        }
    }

    public static String getServiceOfferAttribute(JsonObject serviceOffer, String serviceOfferAttributeName)
            throws ValidationException
    {
        JsonElement serviceOfferAttribute = serviceOffer.get(serviceOfferAttributeName);
        if (serviceOfferAttribute == null) {
            String serviceOfferId = "Unknown";
            try {
                serviceOfferId = serviceOffer.get("id").getAsString();
            } catch (Exception ignored) {}
            throw new ValidationException("Failed to find the attribute '" + serviceOfferAttributeName +
                    "' in the service offer '" + serviceOfferId + "'");
        }

        return serviceOfferAttribute.getAsString();
    }

}
