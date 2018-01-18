package com.sixsq.slipstream.credentials;

import com.google.gson.Gson;

public class CloudCredentialCreateTmpl {

    private static final Gson gson = new Gson();

    public ICloudCredential credentialTemplate;

    public CloudCredentialCreateTmpl(ICloudCredential cd) {
        credentialTemplate = cd;
    }
    public ICloudCredential getCredDef() {
        return credentialTemplate;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static CloudCredentialCreateTmpl fromJson(String json) {
        return gson.fromJson(json, CloudCredentialCreateTmpl.class);
    }
}

