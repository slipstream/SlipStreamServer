package com.sixsq.slipstream.persistence;

import com.google.gson.Gson;

public class UserGeneralParamsTemplate {
    private static final Gson gson = new Gson();
    public UserGeneralParams userParamTemplate;
    public UserGeneralParamsTemplate(UserGeneralParams params) {
        this.userParamTemplate = params;
    }

    public static UserGeneralParamsTemplate fromJson(String jsonRecords) {
        return gson.fromJson(jsonRecords, UserGeneralParamsTemplate.class);
    }
}
