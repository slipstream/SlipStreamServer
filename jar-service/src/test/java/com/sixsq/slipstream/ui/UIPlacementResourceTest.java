package com.sixsq.slipstream.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.util.ResourceTestBase;
import org.junit.BeforeClass;
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

    public static String imageName = "test-image";
    public static String projectName = "test-project";

    @BeforeClass
    public static void setUp() throws ValidationException {
        Module module = new ImageModule(UIPlacementResourceTest.imageName);
        module.store();
        module = new ProjectModule(UIPlacementResourceTest.projectName);
        module.store();
    }

    @Test
    public void putUiPlacementRejectsInvalidJson() throws Exception {
        Response response = putUIPlacement("XXX wrong JSON");
        assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
    }

    @Test
    public void putUiPlacementRejectsEmptyJson() throws Exception {
        Response response = putUIPlacement("{}");
        assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
    }

    @Test
    public void putUiPlacementRejectsJsonWithoutModuleUri() throws Exception {
        String data = "{" +
                "\"placementParams\": {}," +
                "\"userConnectors\": [\"cloud-1\", \"cloud-2\"]" +
                "}";
        Response response = putUIPlacement(data);
        assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
    }

    @Test
    public void putUiPlacementRejectsJsonWithNoneExistentModuleUri() throws
            Exception {
        String data = "{" +
                "\"moduleUri\": \"ImNotThere\"}";
        Response response = putUIPlacement(data);
        assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
    }

    @Test
    public void putUiPlacementRejectsJsonWithProjectModuleUri() throws
            Exception {
        String data = "{" +
                "\"moduleUri\": \"" + projectName + "\"}";
        Response response = putUIPlacement(data);
        assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
    }

    @Test
    public void putUiPlacementReturnsValidJson() throws Exception {

        String data = "{" +
                "\"moduleUri\": \"" + imageName + "\"," +
                "\"placementParams\": {}," +
                "\"userConnectors\": [\"cloud-1\", \"cloud-2\"]" +
                "}";

        Response response = putUIPlacement(data);

        Gson gson = new GsonBuilder().create();
        System.out.println("RESPONSE: " + response.getEntityAsText());
        Object fromJson = gson.fromJson(response.getEntityAsText(), Object.class);

        assertThat(response.getStatus(), is(Status.SUCCESS_OK));
        assertNotNull("The response must be a valid Json", fromJson);
    }

    private Response putUIPlacement(String data) throws Exception {
        Request request = createPutRequest(null, new StringRepresentation(data, MediaType.APPLICATION_ALL_JSON), "ui/placement");
        return executeRequest(request, new UIPlacementResource());
    }

}
