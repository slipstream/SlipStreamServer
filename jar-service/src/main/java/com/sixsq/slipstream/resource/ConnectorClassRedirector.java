package com.sixsq.slipstream.resource;

import org.restlet.Context;

public class ConnectorClassRedirector extends RingRedirector {

	public ConnectorClassRedirector(Context context, String targetPattern, int mode) {
		super(context, targetPattern, mode);
	}
}
