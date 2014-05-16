package com.sixsq.slipstream.connector.stratuslab;

import com.sixsq.slipstream.connector.AbstractConnectorStub;
import com.sixsq.slipstream.connector.Connector;

public class StratusLabConnectorStub extends AbstractConnectorStub {

    public StratusLabConnectorStub() {
        super(StratusLabConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new StratusLabConnector(instanceName);
    }
}
