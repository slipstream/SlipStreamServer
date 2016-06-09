package com.sixsq.slipstream.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.persistence.Module;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java object sent to PRS-lib
 *
 * @see UIPlacementResource
 */
public class PlacementRequest {

    private String moduleUri;

    protected void setModule(Module module) {
        this.module = module;
    }

    public Module getModule(){
        if(module == null) {
            module = Module.load(moduleUri);
        }
        return module;
    }

    private Module module;

    private Map<Object, Object> placementParams;

    private String prsEndPoint;

    private List<String> userConnectors;

    public Map<String, Object> asMap() {

        Map<String, Object> result = new HashMap<>();

        result.put("module", getModule());
        result.put("placement-params", new HashMap<>());
        result.put("prs-endpoint", "");
        result.put("user-connectors", userConnectors);

        return result;
    }

    public static PlacementRequest fromJson(String json) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json, PlacementRequest.class);
    }

    public String toString() {
        return moduleUri + ", " + prsEndPoint + ", " + userConnectors + ", " + placementParams;
    }


}
