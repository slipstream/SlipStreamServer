package com.sixsq.slipstream.connector.okeanos;

import com.sixsq.slipstream.connector.AbstractConnectorStub;
import com.sixsq.slipstream.connector.Connector;

public class OkeanosConnectorStub extends AbstractConnectorStub {

    public OkeanosConnectorStub() {
        super(OkeanosConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new OkeanosConnector(instanceName);
    }
}
