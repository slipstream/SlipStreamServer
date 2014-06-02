package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.connector.AbstractDiscoverableConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class CloudStackDiscoverableConnectorService extends AbstractDiscoverableConnectorService {

    public CloudStackDiscoverableConnectorService() {
        super(CloudStackConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new CloudStackConnector(instanceName);
    }
}
