package com.sixsq.slipstream.connector.physicalhost;

import com.sixsq.slipstream.connector.AbstractDiscoveryConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class PhysicalHostDiscoveryConnectorService extends AbstractDiscoveryConnectorService {

    public PhysicalHostDiscoveryConnectorService() {
        super(PhysicalHostConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new PhysicalHostConnector(instanceName);
    }
}
