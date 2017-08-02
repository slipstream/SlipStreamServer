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

import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceOffersUtil {

    public static String cpuAttributeName = "resource:vcpu";
    public static String ramAttributeName = "resource:ram";
    public static String diskAttributeName = "resource:disk";
    public static String instanceTypeAttributeName = "resource:instanceType";

    private static java.util.logging.Logger logger = Logger.getLogger(ServiceOffersUtil.class.getName());


    public static JsonObject parseJson(String json) {
        if (json == null) return new JsonObject();

        return new JsonParser().parse(json).getAsJsonObject();
    }

    public static JsonObject getServiceOffer(String serviceOfferId) {
        return getServiceOffer(serviceOfferId, false);
    }

    public static JsonObject getServiceOffer(String serviceOfferId, boolean throwIfNotFound) {
        Response response;
        JsonObject res = new JsonObject();
        if (serviceOfferId != null) {
            try {
                response = SscljProxy.get(SscljProxy.BASE_RESOURCE + serviceOfferId, "super ADMIN", true);
            } catch (ResourceException e) {
                if (!throwIfNotFound && e.getStatus().getCode() == 404) {
                    return null;
                }
                throw e;
            }
            res = parseJson(response.getEntityAsText());
        }
        return res;
    }

    public static JsonElement getServiceOfferAttributeOrNull(JsonObject serviceOffer, String serviceOfferAttributeName) {
        if (serviceOffer == null) return null;
        return serviceOffer.get(serviceOfferAttributeName);
    }

    public static JsonElement getServiceOfferAttribute(JsonObject serviceOffer, String serviceOfferAttributeName)
            throws ValidationException {
        JsonElement serviceOfferAttribute = getServiceOfferAttributeOrNull(serviceOffer, serviceOfferAttributeName);

        if (serviceOfferAttribute == null) {
            String serviceOfferId = "Unknown";
            try {

                JsonElement jsonElement = serviceOffer.get("id");

                if (jsonElement == null)
                    throw new ValidationException("[1127] The Json for service offer did not contain any Id attribute while looking for attribute " + serviceOfferAttributeName);

                serviceOfferId = jsonElement.getAsString();

            } catch (Exception ignored) {
                logger.warning("[1127] exception thrown in getServiceOfferAttribute " + ignored);

                logger.warning("[1127] Looking for service attribute named " + serviceOfferAttributeName + " was failing for serviceOfferId " + serviceOfferId);
                throw new ValidationException("Failed to find the attribute '" + serviceOfferAttributeName +
                        "' in the service offer '" + serviceOfferId + "'");
            }

        }

        return serviceOfferAttribute;
    }

    public static String getServiceOfferAttributeAsString(JsonObject serviceOffer, String serviceOfferAttributeName)
            throws ValidationException {
        return getServiceOfferAttribute(serviceOffer, serviceOfferAttributeName).getAsString();
    }

    public static String getServiceOfferAttributeAsStringOrNull(JsonObject serviceOffer, String serviceOfferAttributeName) {
        try {
            return getServiceOfferAttributeAsString(serviceOffer, serviceOfferAttributeName);
        } catch (ValidationException ignored) {
            return null;
        }
    }

}
