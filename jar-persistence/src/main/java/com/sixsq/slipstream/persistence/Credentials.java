package com.sixsq.slipstream.persistence;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class Credentials {
    List<Credential> credentials = new ArrayList<>();
    Credentials() {}
    public static Credentials fromJson(String jsonRecords) {
        return (new Gson()).fromJson(jsonRecords, Credentials.class);
    }
    public List<Credential> getCredentials() {
        return credentials;
    }
}
