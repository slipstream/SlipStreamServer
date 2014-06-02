package com.sixsq.slipstream.connector.openstack;

import com.sixsq.slipstream.connector.AbstractDiscoverableConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class OpenStackDiscoverableConnectorService extends AbstractDiscoverableConnectorService {

    public OpenStackDiscoverableConnectorService() {
        super(OpenStackConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new OpenStackConnector(instanceName);
    }
}
