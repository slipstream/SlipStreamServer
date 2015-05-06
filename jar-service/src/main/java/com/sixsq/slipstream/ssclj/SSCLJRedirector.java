package com.sixsq.slipstream.ssclj;

import static org.restlet.engine.header.HeaderConstants.ATTRIBUTE_HEADERS;

import java.util.logging.Logger;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Reference;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.routing.Redirector;
import org.restlet.routing.Template;
import org.restlet.util.Series;

import com.sixsq.slipstream.event.TypePrincipalRight;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;

public class SSCLJRedirector extends Redirector {

	private static final Logger logger = Logger.getLogger(Redirector.class.getName());
	
	private static final String SLIPSTREAM_AUTHN_INFO = "slipstream-authn-info";

	public SSCLJRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);		
	}

	protected void addSegmentForSSCLJUUID(Reference targetRef, Request request) {
		String sscljUUID = (String) request.getAttributes().get("ssclj-uuid");
		targetRef.addSegment(sscljUUID);
	}

    protected void addOffsetAndLimit(Request request){
        String offset = (String) request.getAttributes().get("offset");
        String limit = (String) request.getAttributes().get("limit");

        request.getResourceRef().addQueryParameter("offset", offset);
        request.getResourceRef().addQueryParameter("limit", limit);
    }

	@SuppressWarnings("unchecked")
	protected void addSlipstreamAuthnInfo(Request request) {
		try {

            @SuppressWarnings("rawtypes")
			Series<Header> requestHeaders = new Series(Header.class);
			request.getAttributes().put(ATTRIBUTE_HEADERS, requestHeaders);

			String username = request.getClientInfo().getUser().getName();
			boolean isSuper = User.loadByName(username).isSuper();
			String role = isSuper ? " " + TypePrincipalRight.ADMIN : "";

			requestHeaders.add(new Header(SLIPSTREAM_AUTHN_INFO, username + role));
		} catch (ValidationException ve) {
			logger.severe("Unable to add SlipstreamAuthnInfo in header:" + ve.getMessage());
		}
	}
	
	// hack inspired by this discussion http://restlet.tigris.org/ds/viewMessage.do?dsForumId=4447&dsMessageId=3076621
	// main trick is to call addSlipstreamAuthnInfo to add slipstream header after it has been removed from request
	//
	protected void serverRedirect(Restlet next, Reference targetRef,
            Request request, Response response) {
        if (next == null) {
            getLogger().warning(
                    "No next Restlet provided for server redirection to "
                            + targetRef);
        } else {
            // Save the base URI if it exists as we might need it for
            // redirections
            Reference resourceRef = request.getResourceRef();
            Reference baseRef = resourceRef.getBaseRef();

            // Reset the protocol and let the dispatcher handle the protocol
            request.setProtocol(null);

            // Update the request to cleanly go to the target URI
            request.setResourceRef(targetRef);
            request.getAttributes().remove(HeaderConstants.ATTRIBUTE_HEADERS);
            
            // hack
            addSlipstreamAuthnInfo(request);
            addOffsetAndLimit(request);
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

}
