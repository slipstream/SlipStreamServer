package com.sixsq.slipstream.connector.stratuslab;

import com.sixsq.slipstream.connector.AbstractDiscoverableConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class StratusLabDiscoverableConnectorService extends AbstractDiscoverableConnectorService {

    public StratusLabDiscoverableConnectorService() {
        super(StratusLabConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new StratusLabConnector(instanceName);
    }
}
