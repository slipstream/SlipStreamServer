package com.sixsq.slipstream.connector.local;

import com.sixsq.slipstream.connector.AbstractDiscoveryConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class LocalDiscoveryConnectorService extends AbstractDiscoveryConnectorService {

    public LocalDiscoveryConnectorService() {
        super(LocalConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new LocalConnector(instanceName);
    }
}
