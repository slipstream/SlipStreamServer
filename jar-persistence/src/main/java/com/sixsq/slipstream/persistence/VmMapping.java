package com.sixsq.slipstream.persistence;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.util.SscljProxy;

import java.util.logging.Logger;

public class VmMapping {

    private static final String VM_MAPPING_RESOURCE = "api/virtual-machine-mapping";

    private static final Logger logger = Logger.getLogger(VmMapping.class.getName());

    private static final Gson gson = new Gson();

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
    private VmMappingServiceOffer serviceOffer;

    public VmMapping(String cloud, String instanceId, String runUuid, String owner, String serviceOffer) {
        if (cloud == null || cloud.isEmpty() ||
                instanceId == null || instanceId.isEmpty() ||
                runUuid == null || runUuid.isEmpty() ||
                owner == null || owner.isEmpty()) {
            throw new IllegalArgumentException("cloud, instanceId, runUuid and owner cannot be null or empty");
        }
        this.cloud = cloud;
        this.instanceId = instanceId;
        this.runUuid = runUuid;
        this.owner = owner;
        if ((serviceOffer != null) && (!serviceOffer.isEmpty())) {
            this.serviceOffer = new VmMappingServiceOffer(serviceOffer);
        } else {
            this.serviceOffer = null;
        }
    }

    public void create() {
        SscljProxy.post(VM_MAPPING_RESOURCE, "internal ADMIN", this);
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static VmMapping fromJson(String json) {
        return gson.fromJson(json, VmMapping.class);
    }

    @Override
    public String toString() {
        return cloud + " " + instanceId + " " + runUuid + " " + owner + " " + serviceOffer;
    }

}
