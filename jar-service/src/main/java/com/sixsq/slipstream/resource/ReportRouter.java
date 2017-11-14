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

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.restlet.security.Authenticator;
import org.restlet.security.Authorizer;

import com.sixsq.slipstream.authn.CookieAuthenticator;
import com.sixsq.slipstream.authz.ReportsAuthorizer;
import com.sixsq.slipstream.authz.SuperEnroler;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.filter.ReportDecorator;

public class ReportRouter extends Router {

	public ReportRouter(Context context, Application application) throws ConfigurationException, ValidationException {
		super(context);

		String reportsLocation = Configuration.getInstance().getProperty("slipstream.reports.location");

		Authorizer authorizer = new ReportsAuthorizer();
		Authenticator authenticator = new CookieAuthenticator(getContext());
		authenticator.setOptional(false);
		authenticator.setEnroler(new SuperEnroler(application));
		authenticator.setNext(authorizer);

		ResultsDirectory directory = new ResultsDirectory(getContext(), "file://" + reportsLocation);
		Filter decorator = new ReportDecorator();
		decorator.setNext(directory);
		authorizer.setNext(decorator);
		attach("", authenticator);
	}

}
