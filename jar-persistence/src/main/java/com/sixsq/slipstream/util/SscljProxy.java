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
import java.util.*;
import java.util.logging.Logger;

public class SscljProxy {

    private static class TestExclStrat implements ExclusionStrategy {
        public boolean shouldSkipClass(Class<?> arg0) {
            return false;
        }

        public boolean shouldSkipField(FieldAttributes f) {
            return f.getName().equals("jpaVersion");
        }
    }

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateTypeAdapter())
            .setPrettyPrinting()
            .setExclusionStrategies(new TestExclStrat())
            .create();

    public enum Method {
        GET,
        PUT,
        POST,
        DELETE;
    }

    public static final String BASE_RESOURCE = "api/";
    public static final String QUOTA_RESOURCE = BASE_RESOURCE + "quota";
    public static final String SERVICE_OFFER_RESOURCE = BASE_RESOURCE + "service-offer";
    public static final String CREDENTIAL_RESOURCE = BASE_RESOURCE + "credential";
    public static final String VIRTUAL_MACHINE_RESOURCE = BASE_RESOURCE + "virtual-machine";

    private static final String SSCLJ_ENDPOINT_PROPERTY_NAME = "ssclj.endpoint";
    private static final String SSCLJ_ENDPOINT_ENV_NAME = "SSCLJ_ENDPOINT";
    private static final String SSCLJ_ENDPOINT_DEFAULT = "http://localhost:8201";
    private static final String ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final Logger logger = Logger.getLogger(SscljProxy.class.getName());

    private static boolean isMuted = false;

    private static final String MUTED_MESSAGE = "You should NOT see this message in production: request to SSCLJ " +
            "won't be made";

    public static Response get(String resource, String authnInfo) {
        return request(Method.GET, resource, null, authnInfo, null, null, null);
    }

    public static Response get(String resource, String authnInfo, Form queryParameters) {
        return request(Method.GET, resource, null, authnInfo, queryParameters, null, null);
    }

    public static Response get(String resource, String authnInfo, Form queryParameters, Boolean throwException) {
        return request(Method.GET, resource, null, authnInfo, queryParameters, null, throwException);
    }

    public static Response get(String resource, String authnInfo, Boolean throwException) {
        return request(Method.GET, resource, null, authnInfo, null, null, throwException);
    }

    public static Response put(String resource, String authnInfo, Object obj) {
        return request(Method.PUT, resource, obj, authnInfo, null, null, null);
    }

    public static Response put(String resource, String authnInfo, Object obj, boolean throwException) {
        return request(Method.PUT, resource, obj, authnInfo, null, null, throwException);
    }

    public static Response post(String resource, Object obj) {
        return request(Method.POST, resource, obj, null, null, null, null);
    }

    public static Response post(String resource, String authnInfo, Object obj) {
        return request(Method.POST, resource, obj, authnInfo, null, null, null);
    }

    public static Response post(String resource, String authnInfo, Boolean throwException) {
        return request(Method.POST, resource, null, authnInfo, null, null, throwException);
    }

    public static Response post(String resource, String authnInfo, Object obj, Boolean throwException) {
        return request(Method.POST, resource, obj, authnInfo, null, null, throwException);
    }

    public static Response post(String resource, MediaType mediaType, Boolean throwException) {
        return request(Method.POST, resource, null, null, null, mediaType, throwException);
    }

    public static Response post(String resource, Form queryParameters, MediaType mediaType, Boolean throwException) {
        return request(Method.POST, resource, null, null, queryParameters, mediaType, throwException);
    }

    public static Response delete(String resource, String authnInfo) {
        return request(Method.DELETE, resource, null, authnInfo, null, null, null);
    }

    public static Response delete(String resource, String authnInfo, Form queryParameters) {
        return request(Method.DELETE, resource, null, authnInfo, queryParameters, null, null);
    }

    public static Response delete(String resource, String authnInfo, Form queryParameters, Boolean throwException) {
        return request(Method.DELETE, resource, null, authnInfo, queryParameters, null, throwException);
    }

    public static Response delete(String resource, String authnInfo, Boolean throwException) {
        return request(Method.DELETE, resource, null, authnInfo, null, null, throwException);
    }

    private static String getSscljEndpoint() {
        String sscljEndpoint = System.getProperty(SSCLJ_ENDPOINT_PROPERTY_NAME,
                System.getenv(SSCLJ_ENDPOINT_ENV_NAME));
        if (sscljEndpoint == null || sscljEndpoint.isEmpty()) {
            sscljEndpoint = SSCLJ_ENDPOINT_DEFAULT;
        }
        return sscljEndpoint;
    }

    private static String queryParametersToString(Iterable<Parameter> parameters) {
        StringBuilder params = new StringBuilder("[");
        if (null != parameters) {
            Iterator i$ = parameters.iterator();

            while (i$.hasNext()) {
                Parameter param = (Parameter) i$.next();
                params.append("name=").append(param.getName().trim()).append(",value=")
                        .append(param.getValue().trim()).append(";");
            }
        }
        return params.append("]").toString();
    }

    private static Response request(Method method, String resource, Object obj, String authnInfo,
                                    Iterable<Parameter> queryParameters, MediaType mediaType,
                                    Boolean throwExceptions) {
        if (isMuted) {
            logger.severe(MUTED_MESSAGE);
            return new Response(new org.restlet.Request());
        }

        ClientResource client = null;
        Response response = null;
        Representation responseEntity = null;
        StringRepresentation content = new StringRepresentation("");
        if (mediaType == null) {
            mediaType = MediaType.APPLICATION_JSON;
        }

        String sscljEndpoint = getSscljEndpoint();

        String requestParamsLog = "method=" + String.valueOf(method)
                + ", resource=" + resource
                + ", object=" + String.valueOf(obj)
                + ", authnInfo=" + authnInfo
                + ", queryParameters=" + queryParametersToString(queryParameters)
                + ", mediaType=" + String.valueOf(mediaType);
        logger.finest("Calling SSCLJ " + sscljEndpoint + " with: " + requestParamsLog);

        try {
            if (obj != null) {
                content = new StringRepresentation(toJson(obj));
                content.setMediaType(mediaType);
            }

            client = new ClientResource(createContext(), sscljEndpoint + "/" + resource);
            client.setRetryOnError(false);
            client.setEntityBuffering(true);

            if (queryParameters != null) {
                client.addQueryParameters(queryParameters);

            }

            if (authnInfo != null) {
                Series<Header> headers = getHeaders(client);
                headers.add("slipstream-authn-info", authnInfo);
            }

            switch (method) {
                case GET:
                    responseEntity = client.get(mediaType);
                    break;
                case PUT:
                    logger.finest("PUT content : " + content.getText());
                    responseEntity = client.put(content, mediaType);
                    break;
                case POST:
                    logger.finest("POST content : " + content.getText());
                    responseEntity = client.post(content, mediaType);
                    break;
                case DELETE:
                    logger.finest("DELETE content : " + content.getText());
                    responseEntity = client.delete(mediaType);
                    break;
                default:
                    throw new UnsupportedOperationException("Method " + method.toString() + "not supported");
            }

            // Hack: to load the response content from the buffer before releasing it.
            // Without this hack it will not be available once this function returns.
            try {
                responseEntity.getText();
            } catch (IOException ignored) {
            }

        } catch (ResourceException re) {
            String message = "ResourceException: " + re.getMessage();
            try {
                Response resp = client.getResponse();
                message += "\n\twith request: " + requestParamsLog;
                message += "\n\twith status: " + resp.getStatus().toString();
                message += "\n\twith content: " + resp.getEntityAsText();
            } catch (Exception ignored) {
            }
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
        return gson.toJson(obj);
    }

    public static JsonObject parseJson(String json) {
        if (json == null) return new JsonObject();

        return new JsonParser().parse(json).getAsJsonObject();
    }

    @SuppressWarnings("unchecked")
    public static Series<Header> getHeaders(Resource resource) {
        Series<Header> headers = (Series<Header>) resource.getRequestAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
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

    public static void muteForTests() {
        isMuted = true;
        logger.severe(MUTED_MESSAGE);
    }

    public static void unmuteForTests() {
        isMuted = false;
    }

    public static boolean isError(Response resp) {
        return resp == null || resp.getStatus().isError();
    }

    public static String respToString(Response resp) {
        return null == resp ? "response is 'null'." : resp.toString();
    }
}
