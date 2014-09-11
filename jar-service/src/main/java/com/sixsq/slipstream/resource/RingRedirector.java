package com.sixsq.slipstream.resource;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;

public class RingRedirector extends Redirector {

	public RingRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);
	}

	@Override
	protected void outboundServerRedirect(Reference targetRef, Request request, Response response) {
		super.outboundServerRedirect(targetRef, request, response);
		if (response.getStatus().isClientError()) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, response.getEntityAsText());
		} else if (response.getStatus().isError()) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, response.getEntityAsText());
		}
	}
}
