package com.sixsq.slipstream.util;

import com.google.gson.Gson;
import com.sixsq.slipstream.connector.SystemConfigurationParametersFactoryBase;
import com.sixsq.slipstream.credentials.CloudCredDef;
import com.sixsq.slipstream.credentials.CredCreateTmpl;
import com.sixsq.slipstream.credentials.ICloudCredDef;
import com.sixsq.slipstream.ssclj.app.SscljTestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.restlet.Response;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloudCredDefTestBase {

    @BeforeClass
    public static void setupClass() {
        SscljTestServer.start();
    }

    @AfterClass
    public static void teardownClass() {
        SscljTestServer.stop();
    }

    public String CONNECTOR_NAME = "foo-bar-baz";

    public String getConnectorName() {
        return CONNECTOR_NAME;
    }

    private void createConnector(String cloudServiceName,
                                 SystemConfigurationParametersFactoryBase sysConfParamsClass) {
            CommonTestUtil.createConnector(
                    cloudServiceName,
                    CONNECTOR_NAME,
                    sysConfParamsClass);
    }

    protected void runCloudCredentialsDirectLifecycle(
            CloudCredDef credCreate, String cloudSerivceName,
            SystemConfigurationParametersFactoryBase sysConfParams) {

        createConnector(cloudSerivceName, sysConfParams);

        CredCreateTmpl credCreateTmpl = new CredCreateTmpl(credCreate);
        Response resp = SscljProxy.post(SscljProxy.BASE_RESOURCE + "credential",
                "test USER", credCreateTmpl);
        assertFalse(resp.toString(), SscljProxy.isError(resp));

        SscljTestServer.refresh();

        HashMap<String, String> response = (new Gson()).fromJson(resp.getEntityAsText(), HashMap.class);
        String resourceId = response.get("resource-id");
        resp = SscljProxy.get(SscljProxy.BASE_RESOURCE + resourceId, "test USER");
        assertFalse(resp.toString(), SscljProxy.isError(resp));

        ICloudCredDef credStored = (ICloudCredDef) CloudCredDef.fromJson(resp.getEntityAsText(),
                credCreate.getClass());
        assertTrue(credCreate.equalsTo(credStored));
    }
}
