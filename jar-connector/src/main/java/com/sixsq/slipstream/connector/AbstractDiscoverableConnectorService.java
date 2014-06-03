package com.sixsq.slipstream.connector;

public abstract class AbstractDiscoverableConnectorService implements DiscoverableConnectorService {

    public final String cloudServiceName;

    public AbstractDiscoverableConnectorService(String cloudServiceName) {
        this.cloudServiceName = cloudServiceName;
    }

    public String getCloudServiceName() {
        return cloudServiceName;
    }

    /**
     * Default implementation is a no-op.
     */
    public void initialize() {
    }

    /**
     * Default implementation is a no-op.
     */
    public void shutdown() {
    }
}
