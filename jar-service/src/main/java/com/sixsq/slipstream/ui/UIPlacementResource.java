package com.sixsq.slipstream.ui;

import com.google.gson.JsonSyntaxException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.resource.BaseResource;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import java.util.logging.Logger;

/**
 * Service called by Javascript.
 * Json (containing userConnectors and moduleURI) is converted in a Java Map.
 * to call PRS-lib static function.
 * Response from PRS-lib is returned as is.
 *
 * @see PlacementRequest
 */
public class UIPlacementResource extends BaseResource {

    private static Logger logger = Logger.getLogger(UIPlacementResource.class.getName());

    @Put("json")
    public Representation generatePrsRequest(Representation data) {
        try {

            String json = data.getText();
            logger.info("PUT data " + json);

            PlacementRequest placementRequest = buildPlacementRequest(json);
            logger.fine("PUT placementRequest as Map" + placementRequest.asMap());

            String prsRequest = generatePrsRequest(placementRequest);
            logger.fine("PUT generated PRS request: " + prsRequest);

            setStatus(Status.SUCCESS_OK);
            return new StringRepresentation(prsRequest, MediaType.APPLICATION_JSON);

        } catch (JsonSyntaxException | ValidationException ex) {
            throw (new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            throw (new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage()));
        }
    }

    protected String generatePrsRequest(PlacementRequest placementRequest)
            throws ValidationException {
        return sixsq.slipstream.prs.core.JavaWrapper.generatePrsRequest(placementRequest.asMap());
    }

    protected PlacementRequest buildPlacementRequest(String json) throws
            ValidationException {
        return PlacementRequest.fromJson(json);
    }

    protected String getPageRepresentation() {
        return "";
    }

}
