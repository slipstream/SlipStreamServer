package com.sixsq.slipstream.connector.stratuslab;

import com.sixsq.slipstream.connector.AbstractDiscoveryConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class StratusLabDiscoveryConnectorService extends AbstractDiscoveryConnectorService {

    public StratusLabDiscoveryConnectorService() {
        super(StratusLabConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new StratusLabConnector(instanceName);
    }
}
