package com.sixsq.slipstream.ui;

import org.restlet.Context;
import org.restlet.routing.Router;

public class UIResourceRouter extends Router {

    public UIResourceRouter(Context context) {
        super(context);
        attach("/placement", UIPlacementResource.class);
    }
}
