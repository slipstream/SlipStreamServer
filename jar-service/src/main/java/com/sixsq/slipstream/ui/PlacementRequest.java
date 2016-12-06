package com.sixsq.slipstream.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Java structure sent to PRS-lib (thanks to asMap method).
 *
 * Responsible to load module object from given URI.
 *
 * @see UIPlacementResource
 */
public class PlacementRequest {

    public static final String SLIPSTREAM_PRS_ENDPOINT_PROPERTY_KEY = "slipstream.prs.endpoint";

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

        Module module = getModule();
        result.put("module", module);
        result.put("user-connectors", this.userConnectors);
        result.put("placement-params", module.placementPoliciesPerComponent());
        result.put("prs-endpoint", prsEndPoint);

        logger.info("asMap = " + result);

        return result;
    }

    public static PlacementRequest fromJson(String json, List<String> userConnectors) {

        logger.info("JSON " + json);

        Gson gson = new GsonBuilder().create();
        PlacementRequest placementRequest = gson.fromJson(json, PlacementRequest.class);

        if (userConnectors != null && (placementRequest.userConnectors == null || placementRequest.userConnectors.isEmpty())) {
            placementRequest.userConnectors = userConnectors;
        }

        try {
            placementRequest.prsEndPoint = Configuration.getInstance().getProperty(SLIPSTREAM_PRS_ENDPOINT_PROPERTY_KEY);
        } catch (ValidationException ve) {
            logger.severe("Unable to determine PRS endpoint. Cause: " + ve.getMessage());
            placementRequest.prsEndPoint = "";
        }
        logger.fine("PRS endpoint " + placementRequest.prsEndPoint);
        logger.info("Placement Request " + placementRequest);

        return placementRequest;
    }

    public String toString() {
        return "moduleURI=" + moduleUri + ", endPoint=" + prsEndPoint + ", userConnectors="
                + userConnectors + ", placementParams=" + placementParams;
    }


}
