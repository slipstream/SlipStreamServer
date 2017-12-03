package com.sixsq.slipstream.credentials;

import com.google.gson.Gson;

public class CloudCredentialCreateTmpl {
    public ICloudCredential credentialTemplate;

    public CloudCredentialCreateTmpl(ICloudCredential cd) {
        credentialTemplate = cd;
    }
    public ICloudCredential getCredDef() {
        return credentialTemplate;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static CloudCredentialCreateTmpl fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, CloudCredentialCreateTmpl.class);
    }
}

