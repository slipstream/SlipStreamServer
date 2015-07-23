package com.sixsq.slipstream.connector;

public class UsageMetric {

    private String metricName;
    private String value;

    public UsageMetric(String metricName, String value){
        this.metricName = metricName;
        this.value = value;
    }

    public UsageMetric(String metricName, Float value){
        this.metricName = metricName;
        this.value = Float.toString(value);
    }

    public UsageMetric(String metricName, Integer value){
        this.metricName = metricName;
        this.value = Integer.toString(value);
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
