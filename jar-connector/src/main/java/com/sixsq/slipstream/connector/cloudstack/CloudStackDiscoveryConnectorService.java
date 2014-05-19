package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.connector.AbstractDiscoveryConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class CloudStackDiscoveryConnectorService extends AbstractDiscoveryConnectorService {

    public CloudStackDiscoveryConnectorService() {
        super(CloudStackConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new CloudStackConnector(instanceName);
    }
}
