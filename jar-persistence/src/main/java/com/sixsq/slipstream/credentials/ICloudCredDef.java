package com.sixsq.slipstream.credentials;

public interface ICloudCredDef<T> {
    String getConnectorInstanceName();
    String toJson();
    boolean equalsTo(ICloudCredDef<T> other);
    boolean credEquals(ICloudCredDef<T> other);
}
