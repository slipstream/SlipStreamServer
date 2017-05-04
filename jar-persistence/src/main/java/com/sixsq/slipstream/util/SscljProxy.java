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

import com.google.gson.*;
import org.restlet.Context;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Resource;
import org.restlet.util.Series;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;


public class SscljProxy {

    public enum Method {
        GET,
        PUT,
        POST;
    }

    private static final String SSCLJ_SERVER = "http://localhost:8201";
    private static final String ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final Logger logger = Logger.getLogger(SscljProxy.class.getName());


    public static Response get(String resource, String username) {
        return request(Method.GET, resource, null, username, null, null, null);
    }

    public static Response get(String resource, String username, Boolean throwException) {
        return request(Method.GET, resource, null, username, null, null, throwException);
    }

    public static Response put(String resource, String username, Object obj) {
        return request(Method.PUT, resource, obj, username, null, null, null);
    }

    public static Response post(String resource, Object obj) {
        return request(Method.POST, resource, obj, null, null, null, null);
    }

    public static Response post(String resource, String username, Object obj) {
        return request(Method.POST, resource, obj, username, null, null, null);
    }

    public static Response post(String resource, MediaType mediaType, Boolean throwException) {
        return request(Method.POST, resource, null, null, null, mediaType, throwException);
    }

    public static Response post(String resource, Form queryParameters, MediaType mediaType, Boolean throwException) {
        return request(Method.POST, resource, null, null, queryParameters, mediaType, throwException);
    }

    private static Response request(Method method, String resource, Object obj, String username,
                                          Iterable<Parameter> queryParameters, MediaType mediaType,
                                          Boolean throwExceptions) {
        ClientResource client = null;
        Response response = null;
        Representation responseEntity = null;
        StringRepresentation content = new StringRepresentation("");

        logger.fine("Calling SSCLJ via HTTP with: "
                + "method=" + String.valueOf(method)
                + ", resource=" + resource
                + ", object=" + String.valueOf(obj)
                + ", username=" + username
                + ", mediaType=" + String.valueOf(mediaType));

        try {
            if (mediaType == null) {
                mediaType = MediaType.APPLICATION_JSON;
            }

            if (obj != null) {
                content = new StringRepresentation(toJson(obj));
                content.setMediaType(mediaType);
            }

            client = new ClientResource(createContext(), SSCLJ_SERVER + "/" + resource);
            client.setRetryOnError(false);
            client.setEntityBuffering(true);

            if (queryParameters != null) {
                client.addQueryParameters(queryParameters);
            }

            if (username != null) {
                Series<Header> headers = getHeaders(client);
                headers.add("slipstream-authn-info", username);
            }

            switch (method) {
                case GET:
                    responseEntity = client.get(mediaType);
                    break;
                case PUT:
                    responseEntity = client.put(content, mediaType);
                    break;
                case POST:
                    responseEntity = client.post(content, mediaType);
                    break;
                default:
                    throw new UnsupportedOperationException("Method " + method.toString() + "not supported");
            }

            // Hack: to load the response content from the buffer before releasing it.
            // Without this hack it will not be available once this function returns.
            try {
                responseEntity.getText();
            } catch (IOException ignored) {}

        } catch (ResourceException re) {
            String message = "ResourceException: " + re.getMessage();
            try {
                Response resp = client.getResponse();
                message += "\n\twith status: " + resp.getStatus().toString();
                message += "\n\twith content: " + resp.getEntityAsText();
            } catch (Exception ignored) {}
            logger.warning(message);
            if (shouldThrow(throwExceptions)) {
                throw re;
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
            if (shouldThrow(throwExceptions)) {
                throw e;
            }
        } finally {
            releaseResources(client, responseEntity);
            logger.fine("SSCLJ HTTP call: resources released");
        }

        if (client != null) {
            response = client.getResponse();
        }

        return response;
    }

    private static boolean shouldThrow(Boolean throwExceptions) {
        return throwExceptions != null && throwExceptions;
    }

    private static Context createContext() {
        Context context = new Context();
        Series<Parameter> parameters = context.getParameters();
        parameters.add("socketTimeout", "1000");
        parameters.add("idleTimeout", "1000");
        parameters.add("idleCheckInterval", "1000");
        parameters.add("socketConnectTimeoutMs", "1000");

        return context;
    }

    private static void releaseResources(ClientResource resource, Representation response) {
        if (response != null) {
            try {
                response.exhaust();
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
            response.release();
        }
        if (resource != null) {
            resource.release();
        }
    }

    public static String toJson(Object obj) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .setPrettyPrinting()
                .create();
        return gson.toJson(obj);
    }

    @SuppressWarnings("unchecked")
    public static Series<Header> getHeaders(Resource resource) {
        Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, headers);
        }
        return headers;
    }

    // See https://code.google.com/p/google-gson/issues/detail?id=281
    public static class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
        private final DateFormat dateFormat;

        private DateTypeAdapter() {
            dateFormat = new SimpleDateFormat(ISO_8601_PATTERN, Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        public synchronized JsonElement serialize(Date date, Type type,
                                                  JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(dateFormat.format(date));
        }

        public synchronized Date deserialize(JsonElement jsonElement, Type type,
                                             JsonDeserializationContext jsonDeserializationContext) {
            try {
                return dateFormat.parse(jsonElement.getAsString());
            } catch (ParseException e) {
                throw new JsonParseException(e);
            }
        }
    }

}
