package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.connector.stratuslab.StratusLabConnector;

public abstract class AbstractConnectorStub implements ConnectorStub {

    public final String cloudServiceName;

    public AbstractConnectorStub(String cloudServiceName) {
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
