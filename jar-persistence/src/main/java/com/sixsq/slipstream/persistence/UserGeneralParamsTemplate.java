package com.sixsq.slipstream.persistence;

import com.google.gson.Gson;

public class UserGeneralParamsTemplate {
    public UserGeneralParams userParamTemplate;
    public UserGeneralParamsTemplate(UserGeneralParams params) {
        this.userParamTemplate = params;
    }

    public static UserGeneralParamsTemplate fromJson(String jsonRecords) {
        return (new Gson()).fromJson(jsonRecords, UserGeneralParamsTemplate.class);
    }
}
