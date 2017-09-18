package com.sixsq.slipstream.authn;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
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

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.ResourceUriUtil;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.security.User;
import org.restlet.util.Series;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class HeaderAuthenticator extends AuthenticatorBase {

    private static final Logger logger = Logger.getLogger(HeaderAuthenticator.class.getName());

    public HeaderAuthenticator(Context context) {
        super(context, false);
    }

    @Override
    protected boolean authenticate(Request request, Response response) {
        if (request.getClientInfo().isAuthenticated()) {
            return true;
        }

        Series<Header> series = (Series<Header>) request.getAttributes().get("org.restlet.http.headers");
        Header authnInfo = series.getFirst("slipstream-authn-info");
        if (authnInfo != null) {
            String username = series.getFirst("slipstream-authn-info").getValue();
            if (username != null && !username.isEmpty()) {
                return handleValid(request, username);
            }
        }
        return handleNotValid(request, response);
    }

    private boolean handleValid(Request request, String username) {

        setClientInfo(request, username);

        com.sixsq.slipstream.persistence.User user = null;
        try {
            user = com.sixsq.slipstream.persistence.User.loadByName(username);
        } catch (ConfigurationException e) {
            return false;
        } catch (ValidationException e) {
            return false;
        }
        if (user == null) {
            return false;
        }

        setUserInRequest(user, request);

        return true;
    }

    private boolean handleNotValid(Request request, Response response) {

        List<MediaType> supported = new ArrayList<MediaType>();
        supported.add(MediaType.APPLICATION_XML);
        supported.add(MediaType.TEXT_HTML);
        MediaType prefered = request.getClientInfo().getPreferredMediaType(supported);

        if (prefered != null && prefered.isCompatible(MediaType.TEXT_HTML)) {
            Reference baseRef = ResourceUriUtil.getBaseRef(request);

            Reference redirectRef = new Reference(baseRef, LoginResource.getResourceRoot());
            redirectRef.setQuery("redirectURL=" + request.getResourceRef().getPath());

            String absolutePath = RequestUtil.constructAbsolutePath(request, redirectRef.toString());

            response.redirectTemporary(absolutePath);
        } else {
            response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
        }

        return false;
    }

    private String setClientInfo(Request request, String username) {
        request.getClientInfo().setAuthenticated(true);

        logger.info("setClientInfo, username = '" + username + "'");

        User user = new User(username);
        request.getClientInfo().setUser(user);
        return username;
    }

}
