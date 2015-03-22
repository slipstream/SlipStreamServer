package com.sixsq.slipstream.event;

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
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;

import com.sixsq.slipstream.exceptions.ValidationException;

public class EventRouter extends Router {

	private static final int PORT_NUMBER = 8201;
	private static final String SSCLJ_SERVER = String.format("http://localhost:%d/ssclj", PORT_NUMBER);

	public static final String ROOT_URI = "event";

	public EventRouter(Context context) throws ValidationException {
		super(context);
			
		String target = SSCLJ_SERVER + "/Event";		
		Redirector listRedirector = new ListEventRedirector(getContext(), target, Redirector.MODE_SERVER_OUTBOUND);		
		Redirector singleRedirector = new SingleEventRedirector(getContext(), target, Redirector.MODE_SERVER_OUTBOUND);
		
		attach("/", listRedirector);		
		attach("/{event-uuid}", singleRedirector);
	}

}
