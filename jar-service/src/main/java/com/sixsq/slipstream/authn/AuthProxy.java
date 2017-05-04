package com.sixsq.slipstream.authn;


import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Context;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Proxy to Authentication Service
 */
public class AuthProxy {

    private static final Logger logger = Logger.getLogger(AuthProxy.class.getName());

    private static final String AUTH_RESOURCE = "auth";

    public static final String INTERNAL_AUTHENTICATION = "internal";
    public static final String GITHUB_AUTHENTICATION = "github";
    public static final String CYCLONE_AUTHENTICATION = "cyclone";

    /**
     * POST to http://localhost:8201/auth/login with username, password and authn-method parameters
     *
     * @param username
     * @param password
     * @param authenticationMethod
     *
     * @return 401 when authentication failed,
     * else for internal authentication a text response contains a cookie with the authentication token
     * else for external authentication forwards the HTML response (typically login page from ID provider)
     *
     * @throws ResourceException
     */
    public Response authenticate(String username, String password, String authenticationMethod)
            throws ResourceException {
        Response response = null;

        try {
            Form queryParameters = new Form();
            queryParameters.add(new Parameter("username", username));
            queryParameters.add(new Parameter("password", password));
            queryParameters.add(new Parameter("authn-method", authenticationMethod));

            response = SscljProxy.post(AUTH_RESOURCE + "/login", queryParameters, MediaType.TEXT_PLAIN, true);

        } catch (ResourceException re) {
            handleResourceException(re, username);
        }

        return response;
    }

    public Response logout() {
        return SscljProxy.post(AUTH_RESOURCE + "/logout", MediaType.TEXT_PLAIN, true);
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

}
