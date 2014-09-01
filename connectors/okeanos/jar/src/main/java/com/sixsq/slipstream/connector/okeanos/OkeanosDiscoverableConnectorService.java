package com.sixsq.slipstream.connector.okeanos;

import com.sixsq.slipstream.connector.AbstractDiscoverableConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class OkeanosDiscoverableConnectorService extends AbstractDiscoverableConnectorService {

    public OkeanosDiscoverableConnectorService() {
        super(OkeanosConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new OkeanosConnector(instanceName);
    }
}
