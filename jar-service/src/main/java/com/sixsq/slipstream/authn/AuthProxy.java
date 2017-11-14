package com.sixsq.slipstream.authn;


import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Response;
import org.restlet.data.MediaType;

import java.util.logging.Logger;

/**
 * Proxy to Authentication Service
 */
public class AuthProxy {

    private static final Logger logger = Logger.getLogger(AuthProxy.class.getName());

    private static final String AUTH_RESOURCE = "auth";

    public static final String INTERNAL_AUTHENTICATION = "internal";

    public Response logout() {
        return SscljProxy.post(AUTH_RESOURCE + "/logout", MediaType.TEXT_PLAIN, true);
    }

}
