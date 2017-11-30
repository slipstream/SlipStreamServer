package com.sixsq.slipstream.credentials;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

import java.util.Map;

public interface ICloudCredential<T> {
    String getConnectorInstanceName();

    String toJson();

    void store(User user) throws ValidationException;

    // TODO:
    // void load(User user) throws ValidationException;

    ICloudCredential fromJson(String json);

    boolean equalsTo(ICloudCredential<T> other);

    boolean credEquals(ICloudCredential<T> other);

    void setParams(Map<String, UserParameter> params);

    Map<String, UserParameter> getParams() throws ValidationException;

    boolean cloudCredsDefined();
}
