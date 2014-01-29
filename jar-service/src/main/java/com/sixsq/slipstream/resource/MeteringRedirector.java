package com.sixsq.slipstream.resource;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.routing.Redirector;

public class MeteringRedirector extends Redirector {

	public MeteringRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);
	}

	protected void outboundServerRedirect(Reference targetRef, Request request,
			Response response) {
		String user = request.getClientInfo().getUser().getName();
		targetRef.addQueryParameter("user_id", user);
		super.outboundServerRedirect(targetRef, request,
				response);
	}

}
