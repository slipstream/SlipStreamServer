package com.sixsq.slipstream.connector.local;

import com.sixsq.slipstream.connector.AbstractDiscoverableConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class LocalDiscoverableConnectorService extends AbstractDiscoverableConnectorService {

    public LocalDiscoverableConnectorService() {
        super(LocalConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new LocalConnector(instanceName);
    }
}
