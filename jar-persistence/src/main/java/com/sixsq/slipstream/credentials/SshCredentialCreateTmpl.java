package com.sixsq.slipstream.credentials;

import com.google.gson.Gson;

public class SshCredentialCreateTmpl {
    private static final Gson gson = new Gson();
    public ISshCredential credentialTemplate;

    public SshCredentialCreateTmpl(ISshCredential cd) {
        credentialTemplate = cd;
    }
    public ISshCredential getCredDef() {
        return credentialTemplate;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static SshCredentialCreateTmpl fromJson(String json) {
        return gson.fromJson(json, SshCredentialCreateTmpl.class);
    }
}

