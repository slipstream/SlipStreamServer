package com.sixsq.slipstream.connector;

import com.google.gson.Gson;

public class CloudConnector {
    private static final Gson gson = new Gson();
    public String cloudServiceType;
    public String instanceName;

    CloudConnector() {
    }

    public static CloudConnector fromJson(String jsonRecords) {
        return gson.fromJson(jsonRecords, CloudConnector.class);
    }
}
