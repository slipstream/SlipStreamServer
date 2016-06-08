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

            // TODO temporary sleep to show loading screen is displayed when run dialog appears
            Thread.sleep(2000);

            String json = data.getText();

            logger.info(">>>> PUT data " + json);
            Gson gson = new GsonBuilder().create();

            PlacementRequest placementRequest = gson.fromJson(json, PlacementRequest.class);
            placementRequest.loadModuleFromUri();

            logger.info(">>>> PUT placementRequest " + placementRequest);
            // TODO call PRS-lib with the placementRequest object

            String jsonOutput = gson.toJson(placementRequest);
            // TODO: unnecessary parse and unparse to validate data in input is valid Json

            return new StringRepresentation(jsonOutput, MediaType.APPLICATION_JSON);

        }catch (JsonSyntaxException jse) {
            throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, jse.getMessage()));
        } catch (Exception e) {
            throw (new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage()));
        }
    }


    protected String getPageRepresentation() {
        return "";
    }
}
