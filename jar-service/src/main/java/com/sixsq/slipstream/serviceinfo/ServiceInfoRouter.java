package com.sixsq.slipstream.serviceinfo;

import org.restlet.Context;
import org.restlet.routing.Router;

public class ServiceInfoRouter extends Router {

    public ServiceInfoRouter(Context context) {
        super(context);

        attach("", ServiceInfoListResource.class);
        attach("/", ServiceInfoListResource.class);

        attach("/{resource-id}", ServiceInfoResource.class);
    }
}
