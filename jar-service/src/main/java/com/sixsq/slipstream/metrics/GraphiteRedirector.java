package com.sixsq.slipstream.metrics;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.routing.Redirector;

public class GraphiteRedirector extends Redirector {

	public GraphiteRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);
	}

	protected void outboundServerRedirect(Reference targetRef, Request request,
			Response response) {
		// Ensure all target parameter begins with 'slipstream.{username}'
		// to avoid any data leakage.
		String username = request.getClientInfo().getUser().getName();
		String[] targets = request.getResourceRef().getQueryAsForm().getValuesArray("target");
		for (String target : targets) {
			if (!target.startsWith("slipstream." + username)) {
				response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					"target parameter must start with 'slipstream." + username + "'");
				return;
			}
		}

		super.outboundServerRedirect(targetRef, request,
				response);
	}

}
