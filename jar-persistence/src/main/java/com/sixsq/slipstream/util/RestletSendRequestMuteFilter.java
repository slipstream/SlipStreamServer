package com.sixsq.slipstream.util;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class RestletSendRequestMuteFilter implements Filter {

	public boolean isLoggable(LogRecord logRecord) {
		return logRecord != null &&
				!("org.restlet.ext.httpclient.internal.HttpMethodCall".equals(logRecord.getSourceClassName()) &&
				"sendRequest".equals(logRecord.getSourceMethodName()));
	}

}
