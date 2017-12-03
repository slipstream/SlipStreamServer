package com.sixsq.slipstream.credentials;

import com.google.gson.Gson;

public class SshCredentialCreateTmpl {
    public ISshCredential credentialTemplate;

    public SshCredentialCreateTmpl(ISshCredential cd) {
        credentialTemplate = cd;
    }
    public ISshCredential getCredDef() {
        return credentialTemplate;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static SshCredentialCreateTmpl fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, SshCredentialCreateTmpl.class);
    }
}

