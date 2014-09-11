package com.sixsq.slipstream.resource;

import org.restlet.Context;
import org.restlet.routing.Redirector;

public class ConnectorInstanceRedirector extends Redirector {

	public ConnectorInstanceRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);
	}
}
