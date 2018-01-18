package com.sixsq.slipstream.persistence;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class VmMappingServiceOffer {

    private static final Gson gson = new Gson();

    @SuppressWarnings("unused")
    @SerializedName("href")
    private String href;

    public VmMappingServiceOffer(String href) {
        if (href == null || href.isEmpty()) {
            throw new IllegalArgumentException("serviceOffer/href cannot be null or empty");
        }
        this.href = href;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static VmMappingServiceOffer fromJson(String json) {
        return gson.fromJson(json, VmMappingServiceOffer.class);
    }

    @Override
    public String toString() {
        return href;
    }

}
