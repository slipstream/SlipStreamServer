package com.sixsq.slipstream.persistence;

class Credential {
    class ConnectorRef {
        public final String href;
        public ConnectorRef(String href) {
            this.href = href;
        }
    }
    public ConnectorRef connector;
    public String id;
    Credential() {}
    public String getConnectorName() {
        if (null != connector && null != connector.href) {
            String[] parts = connector.href.split("/");
            return parts[1];
        } else {
            return "";
        }
    }
}
