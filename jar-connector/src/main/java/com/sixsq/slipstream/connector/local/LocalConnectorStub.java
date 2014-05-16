package com.sixsq.slipstream.connector.local;

import com.sixsq.slipstream.connector.AbstractConnectorStub;
import com.sixsq.slipstream.connector.Connector;

public class LocalConnectorStub extends AbstractConnectorStub {

    public LocalConnectorStub() {
        super(LocalConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new LocalConnector(instanceName);
    }
}
