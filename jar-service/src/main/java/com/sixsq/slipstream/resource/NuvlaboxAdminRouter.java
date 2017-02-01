package com.sixsq.slipstream.resource;

import org.restlet.Context;
import org.restlet.routing.Router;

public class NuvlaboxAdminRouter extends Router {

    public NuvlaboxAdminRouter(Context context) {
        super(context);

        attachNuvlaboxAdmin();

    }

    private void attachNuvlaboxAdmin() {
        attach("", NuvlaboxAdminResource.class);
        attach("/", NuvlaboxAdminResource.class);
    }

}