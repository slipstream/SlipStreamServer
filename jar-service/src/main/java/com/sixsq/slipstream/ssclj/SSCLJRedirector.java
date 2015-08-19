package com.sixsq.slipstream.ssclj;

import com.sixsq.slipstream.event.TypePrincipalRight;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.ResourceUriUtil;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.routing.Redirector;
import org.restlet.routing.Template;
import org.restlet.util.Series;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.restlet.engine.header.HeaderConstants.ATTRIBUTE_HEADERS;

public class SSCLJRedirector extends Redirector {

	private static final Logger logger = Logger.getLogger(Redirector.class.getName());

    private static final String SLIPSTREAM_AUTHN_INFO = "slipstream-authn-info";
    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_FORWARDED_PORT = "X-Forwarded-Port";

    private static final String CIMI_KEY_FIRST_PARAM = "$first";
    private static final String CIMI_KEY_LAST_PARAM = "$last";
    private static final String CIMI_KEY_FILTER_PARAM = "$filter";

    private List<Parameter> cimiParameters;

	public SSCLJRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);
	}

	protected void addSegmentForSSCLJUUID(Reference targetRef, Request request) {
		String sscljUUID = (String) request.getAttributes().get("ssclj-uuid");
		targetRef.addSegment(sscljUUID);
	}

    protected void addSegmentForResourceName(Reference targetRef, Request request) {
        String resourceName = (String) request.getAttributes().get("resourceName");
        targetRef.addSegment(resourceName);
    }

    protected void saveCIMIQueryParams(Request request) {

        this.cimiParameters = new ArrayList<Parameter>();

        String first    = RequestUtil.getQueryValue(request,    CIMI_KEY_FIRST_PARAM);
        String last     = RequestUtil.getQueryValue(request,    CIMI_KEY_LAST_PARAM);
        String filter   = RequestUtil.getQueryValue(request,    CIMI_KEY_FILTER_PARAM);

        checkIntegerPositive(CIMI_KEY_FIRST_PARAM, first);
        checkIntegerPositive(CIMI_KEY_LAST_PARAM, last);

        cimiParameters.add(new Parameter(CIMI_KEY_FIRST_PARAM, first));
        cimiParameters.add(new Parameter(CIMI_KEY_LAST_PARAM, last));
        cimiParameters.add(new Parameter(CIMI_KEY_FILTER_PARAM, filter));
    }


    protected void addCIMIQueryParams(Request request) {
        for(Parameter parameter : cimiParameters) {
            request.getResourceRef().addQueryParameter(parameter);
        }
    }

	@SuppressWarnings("unchecked")
	protected void addSlipStreamHeaders(Request request, Reference baseRef) {
		try {

            @SuppressWarnings("rawtypes")
			Series<Header> requestHeaders = new Series(Header.class);
			request.getAttributes().put(ATTRIBUTE_HEADERS, requestHeaders);

            // this header provides the authentication information to the
            // proxied service
			String username = request.getClientInfo().getUser().getName();
			boolean isSuper = User.loadByName(username).isSuper();
			String role = isSuper ? " " + TypePrincipalRight.ADMIN : "";

            requestHeaders.add(new Header(SLIPSTREAM_AUTHN_INFO, username + role));

            // these headers are required to reconstruct the base URI of the
            // server in the proxied service

            String protocol = baseRef.getScheme(true);
            requestHeaders.add(new Header(X_FORWARDED_PROTO, protocol));

            String host = baseRef.getHostDomain(true);
            requestHeaders.add(new Header(X_FORWARDED_FOR, host));

            int port = baseRef.getHostPort();
            if (port > 0) {
                requestHeaders.add(new Header(X_FORWARDED_PORT, Integer.toString(port)));
            }

        } catch (ValidationException ve) {
			logger.severe("Unable to add headers:" + ve.getMessage());
		}
	}

    protected void outboundServerRedirect(Reference targetRef, Request request, Response response) {
        addSegmentForResourceName(targetRef, request);
        addSegmentForSSCLJUUID(targetRef, request);
        super.outboundServerRedirect(targetRef, request, response);
    }

	// hack inspired by this discussion http://restlet.tigris.org/ds/viewMessage.do?dsForumId=4447&dsMessageId=3076621
	// main trick is to call addSlipStreamHeaders to add slipstream header after it has been removed from request
	//
	protected void serverRedirect(Restlet next, Reference targetRef,
            Request request, Response response) {

        if (next == null) {
            getLogger().warning(
                    "No next Restlet provided for server redirection to "
                            + targetRef);
        } else {

            // hack
            saveCIMIQueryParams(request);

            // Save the base URI if it exists as we might need it for
            // redirections
            Reference resourceRef = request.getResourceRef();
            Reference baseRef = resourceRef.getBaseRef();

            // Save the value before mucking with the request.
            Reference baseRefFromHeaders = ResourceUriUtil.getBaseRef(request);

            // Reset the protocol and let the dispatcher handle the protocol
            request.setProtocol(null);

            // Update the request to cleanly go to the target URI
            request.setResourceRef(targetRef);
            request.getAttributes().remove(HeaderConstants.ATTRIBUTE_HEADERS);

            // hack
            addSlipStreamHeaders(request, baseRefFromHeaders);
            addCIMIQueryParams(request);
            // hack end

            next.handle(request, response);

            // Allow for response rewriting and clean the headers
            response.setEntity(rewrite(response.getEntity()));
            response.getAttributes().remove(HeaderConstants.ATTRIBUTE_HEADERS);
            request.setResourceRef(resourceRef);

            // In case of redirection, we may have to rewrite the redirect URI
            if (response.getLocationRef() != null) {
                Template rt = new Template(this.targetTemplate);
                rt.setLogger(getLogger());
                int matched = rt.parse(response.getLocationRef().toString(),
                        request);

                if (matched > 0) {
                    String remainingPart = (String) request.getAttributes()
                            .get("rr");

                    if (remainingPart != null) {
                        response.setLocationRef(baseRef.toString()
                                + remainingPart);
                    }
                }
            }
        }
    }

    private void checkIntegerPositive(String paramName, String value) {
        if (value != null) {
            try {
                Integer result = new Integer(value);
                if (result < 0) {
                    Util.throwClientBadRequest("The value for '"+ paramName +"' should be positive");
                }
            } catch (NumberFormatException e) {
                Util.throwClientBadRequest("Invalid format for '"+ paramName + "'");
            }
        }
    }

}
