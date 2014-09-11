package com.sixsq.slipstream.resource;

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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration.RequiredParameters;

public class ConnectorInstanceRouter extends Router {

	public ConnectorInstanceRouter(Context context) throws ConfigurationException, ValidationException {
		super(context);

		// contains full url, including different port
		String hostname = Configuration.getInstance()
				.getProperty(RequiredParameters.SLIPSTREAM_RING_HOSTNAME.getName());
		String target = hostname + "/{conn}";

		Redirector redirector;

		// connector instance root detail
		redirector = new ConnectorInstanceRedirector(getContext(), target, Redirector.MODE_SERVER_OUTBOUND);
		attach("/{conn}", redirector);

		// connector instance root
		redirector = new ConnectorInstanceRedirector(getContext(), hostname, Redirector.MODE_SERVER_OUTBOUND);
		attach("", redirector);
	}
}
