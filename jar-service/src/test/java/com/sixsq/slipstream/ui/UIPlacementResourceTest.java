package com.sixsq.slipstream.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.util.ResourceTestBase;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 */
public class UIPlacementResourceTest extends ResourceTestBase {

    @Test
    public void putUiPlacementRejectsInvalidJson() throws Exception {
        Response response = putUIPlacement("XXX wrong JSON", true, false);
        assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
    }

    @Test
    public void putUIPlacementReturn200WhenNotEnabled() throws Exception {
        Response response = putUIPlacement("what ever", false, true);
        assertThat(response.getStatus(), is(Status.SUCCESS_OK));
        assertThat(response.getEntityAsText(), is("{\"message\" : \"PRS not enabled\"}"));
    }

    @Test
    public void putUiPlacementReturnsValidJson() throws Exception {

        String data ="{" +
                "\"moduleUri\": \"unused\"," +
                "\"placementParams\": {}," +
                "\"userConnectors\": [\"cloud-1\", \"cloud-2\"]" +
                "}";

        Response response = putUIPlacement(data, true, false);

        Gson gson = new GsonBuilder().create();
        Object fromJson = gson.fromJson(response.getEntityAsText(), Object.class);

        assertNotNull("The response must be a valid Json", fromJson);
        assertThat(response.getStatus(), is(Status.SUCCESS_OK));
    }

    private Response putUIPlacement(String data, final boolean isEnabled, final boolean throwErrorInPRS) throws Exception {
        Request request = createPutRequest(null, new StringRepresentation(data, MediaType.APPLICATION_ALL_JSON), "ui/placement");

        UIPlacementResource uiPlacementResource = new UIPlacementResource() {
            private Module buildTestModule() {
                try {
                    Module testModule = new com.sixsq.slipstream.persistence.ImageModule("example1");
                    return testModule;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected boolean doCallPRS(){
                return isEnabled;
            }

            @Override
            protected String remotePlaceAndRank(PlacementRequest placementRequest) {
                if(throwErrorInPRS) {
                    throw new Error("Error in PRS");
                } else {
                    return "{}";
                }
            }

            @Override
            protected PlacementRequest buildPlacementRequest(String json) {
                PlacementRequest request = PlacementRequest.fromJson(json);
                request.setModule(buildTestModule());
                return request;
            }
        };

        return executeRequest(request, uiPlacementResource);
    }

}
