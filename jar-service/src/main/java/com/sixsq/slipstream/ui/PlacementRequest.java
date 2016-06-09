package com.sixsq.slipstream.ui;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import com.sixsq.slipstream.persistence.Module;

/**
 * Java object sent to PRS-lib
 *
 * @see UIPlacementResource
 */
public class PlacementRequest {

    @SerializedName("module-uri")
    private String moduleUri;

    private Module module;

    @SerializedName("placement-params")
    private Map<Object, Object> placementParams;

    private String prsEndPoint;

    @SerializedName("user-connectors")
    private List<String> userConnectors;

    public void loadModuleFromUri(){
        this.module = Module.load(moduleUri);
        System.out.println(moduleUri + " :loaded=" + module);
    }

    public String toString() {
        return moduleUri + ", " + prsEndPoint + ", " + userConnectors + ", " + placementParams;
    }

}
