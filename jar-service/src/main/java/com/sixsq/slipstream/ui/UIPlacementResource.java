package com.sixsq.slipstream.ui;

import com.sixsq.slipstream.resource.BaseResource;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

/**
 *
 */
public class UIPlacementResource extends BaseResource {

    @Get("json")
    public Representation getUI() {
        try {
            return new StringRepresentation("{}", MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            throw (new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, e.getMessage()));
        }
    }

    @Put("json")
    public Representation putUI(Representation data) {
        try {


            return new StringRepresentation("{}", MediaType.APPLICATION_JSON);

        } catch (Exception e) {
            throw (new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, e.getMessage()));
        }
    }


    protected String getPageRepresentation() {
        return "";
    }
}
