package com.sixsq.slipstream.connector;

public class UsageMetric {

    private String metricName;
    private String value;

    public UsageMetric(String metricName, String value){
        this.metricName = metricName;
        this.value = value;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getValue() {
        return value;
    }

    public String toString(){
        return metricName + " : " + value;
    }
}
