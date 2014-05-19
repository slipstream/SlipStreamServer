package com.sixsq.slipstream.connector.openstack;

import com.sixsq.slipstream.connector.AbstractDiscoveryConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class OpenStackDiscoveryConnectorService extends AbstractDiscoveryConnectorService {

    public OpenStackDiscoveryConnectorService() {
        super(OpenStackConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new OpenStackConnector(instanceName);
    }
}
