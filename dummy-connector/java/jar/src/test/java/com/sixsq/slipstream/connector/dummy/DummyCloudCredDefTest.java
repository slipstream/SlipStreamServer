package com.sixsq.slipstream.connector.dummy;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.CloudCredDefTestBase;
import org.junit.Test;

import static org.junit.Assert.fail;

public class DummyCloudCredDefTest extends CloudCredDefTestBase {

    @Test
    public void cloudCredentialsDirectLifecycleTest() {
        DummyCloudCredDef credDef = new DummyCloudCredDef(
                getConnectorName(),
                "key",
                "secret",
                "dn");
        DummySystemConfigurationParametersFactory sysConfParams = null;
        try {
            sysConfParams = new
                    DummySystemConfigurationParametersFactory(getConnectorName());
        } catch (ValidationException e) {
            e.printStackTrace();
            fail("Failed to create connector " + CONNECTOR_NAME + " with: " +
                    e.getMessage());
        }
        runCloudCredentialsDirectLifecycle(
                credDef,
                DummyConnector.CLOUD_SERVICE_NAME,
                sysConfParams);
    }
}
