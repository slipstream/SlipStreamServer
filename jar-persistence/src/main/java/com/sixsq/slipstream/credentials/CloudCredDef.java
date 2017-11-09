package com.sixsq.slipstream.credentials;

import com.google.gson.Gson;

public abstract class CloudCredDef<T> implements ICloudCredDef<T> {
    public HRef connector;

    public CloudCredDef(HRef connector) {
        this.connector = connector;
    }

    public String getConnectorInstanceName() {
        return this.connector.getRefResourceName();
    }

    public String toJson() {
        return (new Gson()).toJson(this);
    }

    public static Object fromJson(String json, Class klass) {
            return (new Gson()).fromJson(json, klass);
    }

    public boolean equalsTo(ICloudCredDef<T> other) {
        return getConnectorInstanceName().equals((other)
                .getConnectorInstanceName()) && credEquals(other);
    }
}
