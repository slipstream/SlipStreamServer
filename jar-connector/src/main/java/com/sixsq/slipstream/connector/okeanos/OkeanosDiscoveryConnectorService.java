package com.sixsq.slipstream.connector.okeanos;

import com.sixsq.slipstream.connector.AbstractDiscoveryConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class OkeanosDiscoveryConnectorService extends AbstractDiscoveryConnectorService {

    public OkeanosDiscoveryConnectorService() {
        super(OkeanosConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new OkeanosConnector(instanceName);
    }
}
