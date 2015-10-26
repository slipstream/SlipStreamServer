package com.sixsq.slipstream.resource;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2015 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.representation.Representation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.resource.ServerResource;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.RequestUtil;

import static org.restlet.data.Status.SERVER_ERROR_INTERNAL;

public class RootRedirectResource extends ServerResource {

    private final String defaultRedirectPath = "/dashboard";
    private String redirectPath = defaultRedirectPath;

    public RootRedirectResource() {
        super();
    }

    @Override
    protected void doInit() throws ResourceException {

        try {
            User user = RequestUtil.getUserFromRequest(getRequest());

            // TODO: make the value depend on a user-defined parameter
            redirectPath = "/dashboard";
        } catch (NullPointerException ex) {
            // user not logged-in; redirect to login page
            redirectPath = "/login";
        } catch (ConfigurationException e) {
            throw new ResourceException(SERVER_ERROR_INTERNAL, e.getMessage());
        } catch (ValidationException e) {
            throw new ResourceException(SERVER_ERROR_INTERNAL, e.getMessage());
        }
    }

    public Representation handle() throws ResourceException {
        Request request = getRequest();
        Response response = getResponse();

        String redirectURL;
        if (redirectPath != null) {
            redirectURL = RequestUtil.constructAbsolutePath(request, redirectPath);
        } else {
            redirectURL = RequestUtil.constructAbsolutePath(request, defaultRedirectPath);
        }
        response.redirectSeeOther(redirectURL);

        setResponse(response);
        return new EmptyRepresentation();
    }

}
