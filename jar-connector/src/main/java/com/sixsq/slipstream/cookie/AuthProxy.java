package com.sixsq.slipstream.cookie;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Proxy to Authentication Service
 */
public class AuthProxy {

    private static final Logger logger = Logger.getLogger(AuthProxy.class.getName());

    private static final String AUTH_SERVER = "http://localhost:8202/auth";

    public String createToken(Properties claims, String authenticationToken)
            throws ResourceException {
        ClientResource resource = null;
        Representation response = null;

        try {

            logger.info("Will create token");

            Context context = new Context();
            Series<Parameter> parameters = context.getParameters();
            parameters.add("socketTimeout", "1000");
            parameters.add("idleTimeout", "1000");
            parameters.add("idleCheckInterval", "1000");
            parameters.add("socketConnectTimeoutMs", "1000");

            resource = new ClientResource(context, AUTH_SERVER + "/token");
            resource.setRetryOnError(false);

            Gson gson = new GsonBuilder().create();
            String jsonClaims = gson.toJson(claims);

            resource.addQueryParameter("claims", jsonClaims);
            resource.addQueryParameter("token", authenticationToken);

            response = resource.post("", MediaType.TEXT_PLAIN);

            String token = response.getText();

            logger.info("For " + jsonClaims + ", generated token " + token);

            return token;

        } catch (ResourceException re) {
            handleResourceException(re);
            return null;
        } catch (IOException ioe) {
            logger.info("ioe exception : " + ioe);
            ioe.printStackTrace();
            handleResourceException(new ResourceException(500));
            return null;
        }
        finally {
            releaseResources(resource, response);
        }
    }

    private void handleResourceException(ResourceException re) {
        if(re.getStatus().isConnectorError()) {
            throwConnectionError(re);
        } else {
            throwAuthenticationInvalidToken(re);
        }
    }

    private void throwConnectionError(ResourceException re) {
        logger.severe("Error in contacting authentication server : " + re.getStatus().getDescription());
        throw re;
    }

    private void throwAuthenticationInvalidToken(ResourceException re) {
        String message = "Invalid token provided";
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
