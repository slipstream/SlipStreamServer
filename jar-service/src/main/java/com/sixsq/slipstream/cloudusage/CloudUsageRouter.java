package com.sixsq.slipstream.cloudusage;

import org.restlet.Context;
import org.restlet.routing.Router;

public class CloudUsageRouter extends Router {

    public CloudUsageRouter(Context context) {
        super(context);

        attach("", CloudUsageListResource.class);
        attach("/{cloud}", CloudUsageListResource.class);

        attach("/{resource-id}", CloudUsageResource.class);
    }
}
