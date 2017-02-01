package com.sixsq.slipstream.attribute;

import org.restlet.Context;
import org.restlet.routing.Router;

public class AttributeRouter extends Router {

    public AttributeRouter(Context context) {
        super(context);

        attach("", AttributeListResource.class);
        attach("/", AttributeListResource.class);

        attach("/{resource-id}", AttributeResource.class);
    }
}
