package com.sixsq.slipstream.connector.openstack;

import com.sixsq.slipstream.connector.AbstractConnectorStub;
import com.sixsq.slipstream.connector.Connector;

public class OpenStackConnectorStub extends AbstractConnectorStub {

    public OpenStackConnectorStub() {
        super(OpenStackConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new OpenStackConnector(instanceName);
    }
}
