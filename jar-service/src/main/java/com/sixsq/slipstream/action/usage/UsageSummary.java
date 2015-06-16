package com.sixsq.slipstream.action.usage;

import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public class UsageSummary {

    protected String start_timestamp;
    protected String end_timestamp;
    protected String cloud;
    protected String user;
    protected JsonElement usage;

    protected Map<String, Double> getMetrics() {
        Map<String, Double> result = new HashMap<String, Double>();
        for(Map.Entry<String, JsonElement> entry : usage.getAsJsonObject().entrySet()){
            String metricName = entry.getKey();
            Double metricValue = entry.getValue().getAsJsonObject().get("unit_minutes").getAsDouble();
            result.put(metricName, metricValue);
        }
        return result;
    }

}
