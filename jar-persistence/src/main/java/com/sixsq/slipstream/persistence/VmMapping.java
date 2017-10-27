package com.sixsq.slipstream.persistence;

import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.util.SscljProxy;

import java.util.logging.Logger;

public class VmMapping {

    private static final String USAGE_EVENT_RESOURCE = "api/vm-mapping";

    private static final Logger logger = Logger.getLogger(VmMapping.class.getName());

    @SuppressWarnings("unused")
    @SerializedName("cloud")
    private String cloud;

    @SuppressWarnings("unused")
    @SerializedName("instanceId")
    private String instanceId;

    @SuppressWarnings("unused")
    @SerializedName("runUuid")
    private String runUuid;

    @SuppressWarnings("unused")
    @SerializedName("owner")
    private String owner;

    @SuppressWarnings("unused")
    @SerializedName("serviceOffer")
    private String serviceOffer;

    public VmMapping(String cloud, String instanceId, String runUuid, String owner, String serviceOffer) {
        this.cloud = cloud;
        this.instanceId = instanceId;
        this.runUuid = runUuid;
        this.owner = owner;
        this.serviceOffer = serviceOffer;
    }

    public void create() {
        SscljProxy.post(USAGE_EVENT_RESOURCE, "internal ADMIN", this);
    }

}
