package com.sixsq.slipstream.ssclj;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;

public class SingleSSCLJRedirector extends SSCLJRedirector {

	public SingleSSCLJRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);
	}

	protected void outboundServerRedirect(Reference targetRef, Request request, Response response) {		
		addSegmentForSSCLJUUID(targetRef, request);
		super.outboundServerRedirect(targetRef, request, response);
	}

}
