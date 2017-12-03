package com.sixsq.slipstream.connector;

import com.google.gson.Gson;

public class CloudConnector {
    public String cloudServiceType;
    public String instanceName;

    CloudConnector() {
    }

    public static CloudConnector fromJson(String jsonRecords) {
        return (new Gson()).fromJson(jsonRecords, CloudConnector.class);
    }
}
