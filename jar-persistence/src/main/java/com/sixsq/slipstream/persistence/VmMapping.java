package com.sixsq.slipstream.persistence;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.util.SscljProxy;

import java.util.logging.Logger;

public class VmMapping {

    private static final String VM_MAPPING_RESOURCE = "api/virtual-machine-mapping";

    private static final Logger logger = Logger.getLogger(VmMapping.class.getName());

    @SuppressWarnings("unused")
    @SerializedName("cloud")
    private String cloud;

    @SuppressWarnings("unused")
    @SerializedName("instanceID")
    private String instanceId;

    @SuppressWarnings("unused")
    @SerializedName("runUUID")
    private String runUuid;

    @SuppressWarnings("unused")
    @SerializedName("owner")
    private String owner;

    @SuppressWarnings("unused")
    @SerializedName("serviceOffer")
    private String serviceOffer;

    public VmMapping(String cloud, String instanceId, String runUuid, String owner, String serviceOffer) {
        if (cloud == null || instanceId == null) {
            throw new IllegalArgumentException("cloud and instanceId cannot be null");
        }
        this.cloud = cloud;
        this.instanceId = instanceId;
        this.runUuid = runUuid;
        this.owner = owner;
        this.serviceOffer = serviceOffer;
    }

    public void create() {
        SscljProxy.post(VM_MAPPING_RESOURCE, "internal ADMIN", this);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static VmMapping fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, VmMapping.class);
    }

    @Override
    public String toString() {
        return cloud + " " + instanceId + " " + runUuid + " " + owner + " " + serviceOffer;
    }

}
