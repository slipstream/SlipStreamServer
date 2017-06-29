package com.sixsq.slipstream.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Java structure sent to PRS-lib (thanks to asMap method).
 * <p>
 * Responsible to load module object from given URI.
 *
 * @see UIPlacementResource
 */
public class PlacementRequest {

    private static Logger logger = Logger.getLogger(PlacementRequest.class.getName());

    private String moduleUri;

    protected void setModule(Module module) {
        this.module = module;
    }

    public Module getModule() throws ValidationException {
        if (module == null) {
            if (moduleUri == null) {
                throw (new ValidationException("moduleUri should be set."));
            } else {
                module = Module.load(moduleUri);
            }
        }

        if (module == null) {
            throw (new ValidationException("Failed to load module: " +
                    moduleUri));
        }
        if (module.getCategory() != ModuleCategory.Deployment &&
                module.getCategory() != ModuleCategory.Image) {
            throw (new ValidationException("Provided module " + moduleUri +
                    " should be either Component or Application."));
        }
        logger.fine("Loaded module " + module);
        return module;
    }

    private Module module;

    private Map<Object, Object> placementParams;

    private List<String> userConnectors;

    private boolean isScalable;

    public Map<String, Object> asMap() throws ValidationException {

        Map<String, Object> result = new HashMap<>();

        Module module = getModule();
        result.put("module", module);
        result.put("user-connectors", userConnectors);
        result.put("isScalable", isScalable);
        result.put("placement-params", module.placementPoliciesPerComponent());

        logger.info("asMap = " + result);

        return result;
    }

    public static PlacementRequest fromJson(String json) throws ValidationException {
        Gson gson = new GsonBuilder().create();
        PlacementRequest placementRequest = gson.fromJson(json, PlacementRequest.class);

        validateFromJSON(placementRequest);

        logger.fine("Placement Request " + placementRequest);

        return placementRequest;
    }

    public String toString() {
        return "moduleURI=" + moduleUri + ", userConnectors="
                + userConnectors + ", placementParams=" + placementParams +
                ", isScalable=" + isScalable;
    }

    public static void validateFromJSON(PlacementRequest pr) throws
            ValidationException {
        if (null == pr.moduleUri) {
            throw (new ValidationException("moduleUri should be set."));
        }
    }
}
