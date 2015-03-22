package com.sixsq.slipstream.event;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;

public class SingleEventRedirector extends EventRedirector {

	public SingleEventRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);
	}

	protected void outboundServerRedirect(Reference targetRef, Request request, Response response) {		
		addSegmentForEventUUID(targetRef, request);
		super.outboundServerRedirect(targetRef, request, response);
	}

}
