package com.sixsq.slipstream.credentials;

import com.google.gson.Gson;

public class CredCreateTmpl {
    public ICloudCredDef credentialTemplate;

    public CredCreateTmpl(ICloudCredDef cd) {
        credentialTemplate = cd;
    }
    public ICloudCredDef getCredDef() {
        return credentialTemplate;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static CredCreateTmpl fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, CredCreateTmpl.class);
    }
}

