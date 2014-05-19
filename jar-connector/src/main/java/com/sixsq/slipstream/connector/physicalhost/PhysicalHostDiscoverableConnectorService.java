package com.sixsq.slipstream.connector.physicalhost;

import com.sixsq.slipstream.connector.AbstractDiscoverableConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class PhysicalHostDiscoverableConnectorService extends AbstractDiscoverableConnectorService {

    public PhysicalHostDiscoverableConnectorService() {
        super(PhysicalHostConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new PhysicalHostConnector(instanceName);
    }
}
