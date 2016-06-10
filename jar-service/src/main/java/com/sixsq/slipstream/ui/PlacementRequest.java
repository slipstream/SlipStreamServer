package com.sixsq.slipstream.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sixsq.slipstream.persistence.Module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Java object sent to PRS-lib
 *
 * @see UIPlacementResource
 */
public class PlacementRequest {

    private static Logger logger = Logger.getLogger(PlacementRequest.class.getName());

    private String moduleUri;

    protected void setModule(Module module) {
        this.module = module;
    }

    public Module getModule(){
        if(module == null) {
            module = Module.load(moduleUri);
        }

        logger.fine("Loaded module " + module);
        return module;
    }

    private Module module;

    private Map<Object, Object> placementParams;

    private String prsEndPoint;

    private List<String> userConnectors;

    public Map<String, Object> asMap() {

        Map<String, Object> result = new HashMap<>();

        result.put("module", getModule());
        result.put("user-connectors", userConnectors);

        result.put("placement-params", new HashMap<>());
        result.put("prs-endpoint", "");

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
