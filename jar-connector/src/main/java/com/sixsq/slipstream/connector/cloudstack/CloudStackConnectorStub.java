package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.connector.AbstractConnectorStub;
import com.sixsq.slipstream.connector.Connector;

public class CloudStackConnectorStub extends AbstractConnectorStub {

    public CloudStackConnectorStub() {
        super(CloudStackConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new CloudStackConnector(instanceName);
    }
}
