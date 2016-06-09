package com.sixsq.slipstream.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sixsq.slipstream.resource.BaseResource;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class UIPlacementResource extends BaseResource {

    private static Logger logger = Logger.getLogger(UIPlacementResource.class.getName());

    @Get("json")
    public Representation getUI() {
        try {
            logger.fine("GET req " + getRequest());
            logger.fine("GET response " + getResponse());
            return new StringRepresentation("{}", MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            throw (new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, e.getMessage()));
        }
    }

    @Put("json")
    public Representation putUI(Representation data) {
        try {

            String json = data.getText();

            logger.info(">>>> PUT data " + json);

            PlacementRequest placementRequest = buildPlacementRequest(json);
            logger.info(">>>> PUT placementRequest " + placementRequest);
            logger.info(">>>> PUT placementRequest as Map" + placementRequest.asMap());

            String prsLibRes = sixsq.slipstream.prs.core.JavaWrapper.placeAndRank(placementRequest.asMap());
            logger.info(">>>> PUT result of call to PRS lib : " + prsLibRes);

            return new StringRepresentation(prsLibRes, MediaType.APPLICATION_JSON);

        }catch (JsonSyntaxException jse) {
            throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, jse.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            throw (new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage()));
        }
    }

    protected PlacementRequest buildPlacementRequest(String json){
        return PlacementRequest.fromJson(json);
    }


    protected String getPageRepresentation() {
        return "";
    }
}
