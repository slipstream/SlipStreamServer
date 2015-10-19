package com.sixsq.slipstream.authn;


import org.restlet.Context;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Proxy to Authentication Service
 */
public class AuthProxy {

    private static final Logger logger = Logger.getLogger(AuthProxy.class.getName());

    private static final String AUTH_SERVER = "http://localhost:8202/auth";

    public Response createAuthnToken(String username, String password)
            throws ResourceException {
        ClientResource resource = null;
        Representation response = null;

        try {

            Context context = new Context();
            Series<Parameter> parameters = context.getParameters();
            parameters.add("socketTimeout", "1000");
            parameters.add("idleTimeout", "1000");
            parameters.add("idleCheckInterval", "1000");
            parameters.add("socketConnectTimeoutMs", "1000");

            resource = new ClientResource(context, AUTH_SERVER + "/login");
            resource.setRetryOnError(false);

            resource.addQueryParameter("user-name", username);
            resource.addQueryParameter("password", password);

            response = resource.post("", MediaType.TEXT_PLAIN);
            logger.info("Successful connection for '" + username + "'");

            return resource.getResponse();

        } catch (ResourceException re) {
            handleResourceException(re, username);
            return null;
        } finally {
            releaseResources(resource, response);
        }
    }

    private void handleResourceException(ResourceException re, String username) {
        if(re.getStatus().isConnectorError()) {
            throwConnectionError(re);
        } else {
            throwAuthenticationError(re, username);
        }
    }

    private void throwConnectionError(ResourceException re) {
        logger.severe("Error in contacting authentication server : " + re.getStatus().getDescription());
        throw re;
    }

    private void throwAuthenticationError(ResourceException re, String username) {
        boolean noUserName = (username==null || username.isEmpty());
        String message = noUserName ?
                "No user name provided" :
                String.format("Username/password combination not valid for user '%s'", username);

        logger.warning(message);
        throw re;
    }

    private void releaseResources(ClientResource resource, Representation response) {
        if (response != null) {
            try {
                response.exhaust();
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
            response.release();
        }
        if (resource != null) {
            resource.release();
        }
    }

}
