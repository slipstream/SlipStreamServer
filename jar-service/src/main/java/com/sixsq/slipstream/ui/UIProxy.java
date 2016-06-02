package com.sixsq.slipstream.ui;

import com.sixsq.slipstream.sscljproxy.SscljProxy;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 *
 */
public class UIProxy extends SscljProxy {

    protected String namespace() {
        return "ui";
    }

    @Get("json")
    public Representation toJson() {
        try {
            return new StringRepresentation(putSsclj(), MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            String message = "Unable to contact SSCLJ Server: " + e.getMessage();
            throw (new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message));
        }
    }

    protected String uri() {
        String serviceName = (String) getRequest().getAttributes().get("service-name");
        return SSCLJ_SERVER_BASE_URL + "/" + namespace() + "/" + serviceName;
    }

    protected String getPageRepresentation() {
        return "";
    }
}
