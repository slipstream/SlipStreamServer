package com.sixsq.slipstream.application;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.util.RequestUtil;

public class RequestViewer extends Restlet {

	private final String name;

	public RequestViewer(String name) {
		super();
		this.name = name;
	}

	public RequestViewer(Context context, String name) {
		super(context);
		this.name = name;
	}

	@Override
	public void handle(Request request, Response response) {

		String msg = representRequestAsString(request);
		response.setEntity(msg, MediaType.TEXT_PLAIN);
	}

	private String representRequestAsString(Request request) {

		StringBuilder msg = new StringBuilder(name + ':' + '\n');
		msg.append(representRawRequestAsString(request));

		return msg.toString();
	}

	public static String representRawRequestAsString(Request request) {

		Reference hostRef = request.getHostRef();
		Reference resourceRef = request.getResourceRef();
		Reference rootRef = request.getRootRef();

		Configuration configuration = RequestUtil
				.getConfigurationFromRequest(request);

		StringBuilder msg = new StringBuilder();
		msg.append("Host URI  : " + hostRef + '\n');
		msg.append("Res. URI  : " + resourceRef + '\n');
		msg.append("Root URI  : " + rootRef + '\n');
		msg.append("Routed    : " + resourceRef.getBaseRef() + '\n');
		msg.append("Remaining : " + resourceRef.getRemainingPart() + '\n');

		if (configuration != null) {
			msg.append("Config.   : "
					+ configuration.getClass().getCanonicalName() + '\n');
		} else {
			msg.append("Config.   : NULL\n");
		}

		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		msg.append("\n");
		msg.append(CookieUtils.cookieToString(cookie));

		return msg.toString();
	}
}
