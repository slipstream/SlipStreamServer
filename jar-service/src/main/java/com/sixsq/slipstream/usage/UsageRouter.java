package com.sixsq.slipstream.usage;

import org.restlet.Context;
import org.restlet.routing.Router;

public class UsageRouter extends Router {

    public UsageRouter(Context context) {
        super(context);

        attach("", UsageListResource.class);
        attach("/", UsageListResource.class);

        attach("/{resource-id}", UsageResource.class);
    }
}
