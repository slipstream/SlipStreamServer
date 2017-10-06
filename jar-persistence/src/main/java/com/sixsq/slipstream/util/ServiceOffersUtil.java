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
import org.restlet.data.Form;

import java.util.logging.Logger;

import static com.sixsq.slipstream.util.SscljProxy.parseJson;

public class ServiceOffersUtil {

    public static String cpuAttributeName = "resource:vcpu";
    public static String ramAttributeName = "resource:ram";
    public static String diskAttributeName = "resource:disk";
    public static String instanceTypeAttributeName = "resource:instanceType";

    private static java.util.logging.Logger logger = Logger.getLogger(ServiceOffersUtil.class.getName());

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
            if (response == null) {
                return null;
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
                serviceOfferId = jsonElement.getAsString();
            } catch (Exception ignored) {
                logger.warning("ServiceOffer exception: " + ignored);
            }
            throw new ValidationException("Failed to find the attribute '" + serviceOfferAttributeName +
                    "' in the service offer '" + serviceOfferId + "'");
        }

        return serviceOfferAttribute;
    }

    public static String getServiceOfferAttributeAsString(JsonObject serviceOffer, String serviceOfferAttributeName)
            throws ValidationException {
        return getServiceOfferAttribute(serviceOffer, serviceOfferAttributeName).getAsString();
    }

    public static String getServiceOfferAttributeAsStringOrNull(JsonObject serviceOffer, String serviceOfferAttributeName) {
        JsonElement serviceOfferAttribute = getServiceOfferAttributeOrNull(serviceOffer, serviceOfferAttributeName);
        if (serviceOfferAttribute != null) {
            return serviceOfferAttribute.getAsString();
        }
        return null;
    }

    public static Integer getServiceOfferAttributeAsIntegerOrNull(JsonObject serviceOffer, String serviceOfferAttributeName) {
        JsonElement serviceOfferAttribute = getServiceOfferAttributeOrNull(serviceOffer, serviceOfferAttributeName);
        if (serviceOfferAttribute != null) {
            return serviceOfferAttribute.getAsInt();
        }
        return null;
    }

    public static Float getServiceOfferAttributeAsFloatOrNull(JsonObject serviceOffer, String serviceOfferAttributeName) {
        JsonElement serviceOfferAttribute = getServiceOfferAttributeOrNull(serviceOffer, serviceOfferAttributeName);
        if (serviceOfferAttribute != null) {
            return serviceOfferAttribute.getAsFloat();
        }
        return null;
    }

    public static JsonObject getServiceOffer(String cloud, Integer cpu, Float ram, Float disk, String instanceType) {
        String filter = "connector/href=\"" + cloud + "\" ";
        if (cpu != null){
            filter += " and " + cpuAttributeName + "=" + cpu;
        }
        if (ram != null){
            filter += " and " + ramAttributeName + "=" + (int) ram.floatValue();
        }
        if (disk != null && disk > 0){
            filter += " and " + diskAttributeName + "=" + (int) disk.floatValue();
        }
        if (instanceType != null){
            filter += " and " + instanceTypeAttributeName + "=\"" + instanceType + "\"";
        }
        Form queryParameters = new Form();
        queryParameters.add("$filter", filter);
        queryParameters.add("$orderby", "price:unitCost");

        Response response = SscljProxy.get(SscljProxy.SERVICE_OFFER_RESOURCE, "super ADMIN", queryParameters);

        if (response == null) {
            return null;
        }
        return parseJson(response.getEntityAsText());
    }

}
