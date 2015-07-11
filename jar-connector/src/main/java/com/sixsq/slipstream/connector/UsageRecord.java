package com.sixsq.slipstream.connector;

import com.google.gson.*;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class UsageRecord {

    private static final String SSCLJ_SERVER = "http://localhost:8201/api";

    private static final Logger logger = Logger.getLogger(UsageRecord.class.getName());

    private static final String ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @SuppressWarnings("unused")
    private ACL acl;

    @SuppressWarnings("unused")
    private String user;

    @SuppressWarnings("unused")
    private String cloud;

    @SuppressWarnings("unused")
    private Date start_timestamp;

    @SuppressWarnings("unused")
    private Date end_timestamp;

    @SuppressWarnings("unused")
    private String metric_name;

    @SuppressWarnings("unused")
    private String metric_value;

    @SuppressWarnings("unused")
    private String cloud_vm_instanceid;

    private List<Map<String, String>> metrics;

    public UsageRecord(ACL acl, String user, String cloud, String cloud_vm_instanceid,
                       Date start_timestamp, Date end_timestamp, List<Map<String, String>> metrics) {
        this.acl = acl;
        this.user = user;
        this.cloud = cloud;
        this.cloud_vm_instanceid = cloud_vm_instanceid;
        this.start_timestamp = start_timestamp;
        this.end_timestamp = end_timestamp;
        this.metrics = metrics;
    }

    public String toJson(){

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .setPrettyPrinting()
                .create();

        return gson.toJson(this);
    }

    public static void post(UsageRecord usageRecord) {
        ClientResource resource = null;
        Representation response = null;

        try {
            StringRepresentation stringRep = new StringRepresentation(usageRecord.toJson());
            stringRep.setMediaType(MediaType.APPLICATION_JSON);

            Context context = new Context();
            Series<Parameter> parameters = context.getParameters();
            parameters.add("socketTimeout", "1000");
            parameters.add("idleTimeout", "1000");
            parameters.add("idleCheckInterval", "1000");
            parameters.add("socketConnectTimeoutMs", "1000");

            resource = new ClientResource(context, SSCLJ_SERVER + "/usage-record");
            resource.setRetryOnError(false);

            response = resource.post(stringRep, MediaType.APPLICATION_JSON);

        } catch (ResourceException re) {
            logger.warning("ResourceException :" + re.getMessage());
        } catch (Exception e) {
            logger.warning(e.getMessage());
        } finally {
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
    }

    // See https://code.google.com/p/google-gson/issues/detail?id=281
    private static class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
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
