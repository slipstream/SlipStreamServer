package com.sixsq.slipstream.module;

import org.restlet.Context;
import org.restlet.routing.Router;

import com.sixsq.slipstream.module.ModuleListResource;

public class ModuleListRouter extends Router {

    public ModuleListRouter(Context context, Class<? extends ModuleListResource> resourceClass) {
        super(context);

        attach("", resourceClass);
        attach("/", resourceClass);
    }

}
