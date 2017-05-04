package com.sixsq.slipstream.connector;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.engine.header.Header;
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

public class UsageEvent {

    private static final String USAGE_EVENT_RESOURCE = "api/usage-event";

    private static final Logger logger = Logger.getLogger(UsageEvent.class.getName());

    @SuppressWarnings("unused")
    private ACL acl;

    @SuppressWarnings("unused")
    private String user;

    @SuppressWarnings("unused")
    private String cloud;

    @SuppressWarnings("unused")
    @SerializedName("start-timestamp")
    private Date start_timestamp;

    @SuppressWarnings("unused")
    @SerializedName("end-timestamp")
    private Date end_timestamp;

    @SuppressWarnings("unused")
    @SerializedName("metric-name")
    private String metric_name;

    @SuppressWarnings("unused")
    @SerializedName("metric-value")
    private String metric_value;

    @SuppressWarnings("unused")
    @SerializedName("cloud-vm-instanceid")
    private String cloud_vm_instanceid;

    private List<Map<String, String>> metrics;

    public UsageEvent(ACL acl, String user, String cloud, String cloud_vm_instanceid,
                      Date start_timestamp, Date end_timestamp, List<UsageMetric> metrics) {
        this.acl = acl;
        this.user = user;
        this.cloud = cloud;
        this.cloud_vm_instanceid = cloud_vm_instanceid;
        this.start_timestamp = start_timestamp;
        this.end_timestamp = end_timestamp;
        this.metrics = convert(metrics);
    }

    private List<Map<String, String>> convert(List<UsageMetric> usageMetrics){
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();

        for(UsageMetric usageMetric : usageMetrics) {
            Map<String, String> metricMap = new HashMap<String, String>();
            metricMap.put("name", usageMetric.getMetricName());
            metricMap.put("value", usageMetric.getValue());
            result.add(metricMap);
        }
        return result;
    }

    public String toJson(){
        return SscljProxy.toJson(this);
    }

    public static void post(UsageEvent usageEvent) {
        SscljProxy.post(USAGE_EVENT_RESOURCE, usageEvent.user, usageEvent);
    }

}
