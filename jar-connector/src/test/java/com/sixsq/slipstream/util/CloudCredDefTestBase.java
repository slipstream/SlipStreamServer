package com.sixsq.slipstream.util;

import com.google.gson.Gson;
import com.sixsq.slipstream.connector.SystemConfigurationParametersFactoryBase;
import com.sixsq.slipstream.credentials.CloudCredential;
import com.sixsq.slipstream.credentials.CloudCredentialCreateTmpl;
import com.sixsq.slipstream.credentials.ICloudCredential;
import com.sixsq.slipstream.ssclj.app.CIMITestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.restlet.Response;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloudCredDefTestBase {

    private static final Gson gson = new Gson();

    @BeforeClass
    public static void setupClass() {
        CIMITestServer.start();
    }

    @AfterClass
    public static void teardownClass() {
        CIMITestServer.stop();
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
            CloudCredential credCreate, String cloudSerivceName,
            SystemConfigurationParametersFactoryBase sysConfParams) {

        createConnector(cloudSerivceName, sysConfParams);

        CloudCredentialCreateTmpl cloudCredentialCreateTmpl = new CloudCredentialCreateTmpl(credCreate);
        Response resp = SscljProxy.post(SscljProxy.BASE_RESOURCE + "credential",
                "test USER", cloudCredentialCreateTmpl);
        assertFalse(resp.toString(), SscljProxy.isError(resp));

        CIMITestServer.refresh();

        HashMap<String, String> response = gson.fromJson(resp.getEntityAsText(), HashMap.class);
        String resourceId = response.get("resource-id");
        resp = SscljProxy.get(SscljProxy.BASE_RESOURCE + resourceId, "test USER");
        assertFalse(resp.toString(), SscljProxy.isError(resp));

        ICloudCredential credStored = (ICloudCredential) CloudCredential.fromJson(resp.getEntityAsText(),
                credCreate.getClass());
        assertTrue(credCreate.equalsTo(credStored));
    }
}
