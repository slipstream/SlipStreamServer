package com.sixsq.slipstream.credentials;

public class HRef {
    public String href;

    public HRef(String href) {
        this.href = href;
    }
    public String getRefResourceName() {
        if (null == this.href) {
            return "";
        } else {
            String[] parts = this.href.split("/");
            if (parts.length < 2) {
                return "";
            } else {
                return parts[1];
            }
        }
    }
}
