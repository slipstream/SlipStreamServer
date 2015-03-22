package com.sixsq.slipstream.event;

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

public class EventRedirector extends Redirector {

	private static final String SLIPSTREAM_AUTHN_INFO = "slipstream-authn-info";

	public EventRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);		
	}

	protected void addSegmentForEventUUID(Reference targetRef, Request request) {
		String eventUUID = (String) request.getAttributes().get("event-uuid");
		targetRef.addSegment(eventUUID);
	}
	
	protected void addSlipstreamAuthnInfo(Request request) {		
		Series<Header> requestHeaders = new Series(Header.class);
		request.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, requestHeaders);
		
		// TODO check not already existing
		String username = request.getClientInfo().getUser().getName();
		// roles should be added when available after username, space separated, e.g "username role1 role2 ..."
				
		requestHeaders.add(new Header(SLIPSTREAM_AUTHN_INFO, username));				
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
