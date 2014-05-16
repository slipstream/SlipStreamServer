package com.sixsq.slipstream.connector.physicalhost;

import com.sixsq.slipstream.connector.AbstractConnectorStub;
import com.sixsq.slipstream.connector.Connector;

public class PhysicalHostConnectorStub extends AbstractConnectorStub {

    public PhysicalHostConnectorStub() {
        super(PhysicalHostConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new PhysicalHostConnector(instanceName);
    }
}
