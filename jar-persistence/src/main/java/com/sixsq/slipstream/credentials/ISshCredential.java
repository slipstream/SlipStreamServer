package com.sixsq.slipstream.credentials;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

import java.util.Map;

public interface ISshCredential<T> {
    String toJson();

    void store(User user) throws ValidationException;

    void load(User user) throws ValidationException;

    ISshCredential fromJson(String json);

    boolean equalsTo(ISshCredential<T> other);

    boolean credEquals(ISshCredential<T> other);

    void setParams(Map<String, UserParameter> params);

    Map<String, UserParameter> getParams() throws ValidationException;
}
