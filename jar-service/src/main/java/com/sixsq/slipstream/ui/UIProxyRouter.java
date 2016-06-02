package com.sixsq.slipstream.ui;

import org.restlet.Context;
import org.restlet.routing.Router;

public class UIProxyRouter extends Router {

    public UIProxyRouter(Context context) {
        super(context);
        attach("/{service-name}", UIProxy.class);
    }
}
