package com.sixsq.slipstream.connector.cloudstack;

import com.sixsq.slipstream.connector.AbstractDiscoverableConnectorService;
import com.sixsq.slipstream.connector.Connector;

public class CloudStackAdvancedZoneDiscoverableConnectorService extends AbstractDiscoverableConnectorService {

    public CloudStackAdvancedZoneDiscoverableConnectorService() {
        super(CloudStackAdvancedZoneConnector.CLOUD_SERVICE_NAME);
    }

    public Connector getInstance(String instanceName) {
        return new CloudStackAdvancedZoneConnector(instanceName);
    }
}
