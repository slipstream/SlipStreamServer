package com.sixsq.slipstream.persistence;

import com.google.gson.Gson;

class CloudConnector {
    public String cloudServiceType;
    public String instanceName;
    CloudConnector() {}
    public static CloudConnector fromJson(String jsonRecords) {
        return (new Gson()).fromJson(jsonRecords, CloudConnector.class);
    }
}
