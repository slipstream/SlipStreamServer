package com.sixsq.slipstream.filter;

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

import java.util.LinkedList;
import java.util.List;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.routing.Filter;

public class TrimmedMediaTypesFilter extends Filter {

	private final static String[] affectedBrowserNames = { "IE", "iPad", "iPhone" };

	public TrimmedMediaTypesFilter(Context context, Restlet next) {
		super(context, next);
	}

	@Override
	protected int beforeHandle(Request request, Response response) {

		ClientInfo clientInfo = request.getClientInfo();
		String agent = clientInfo.getAgent();

		for (String browserName : affectedBrowserNames) {
			if (agent != null && agent.contains(browserName)) {

				List<Preference<MediaType>> preferences;
				preferences = clientInfo.getAcceptedMediaTypes();

				LinkedList<Preference<MediaType>> trimmedPreferences;
				trimmedPreferences = new LinkedList<Preference<MediaType>>();

				for (Preference<MediaType> preference : preferences) {
					MediaType mediaType = preference.getMetadata();
					if (MediaType.TEXT_HTML.equals(mediaType)
							|| MediaType.APPLICATION_XHTML.equals(mediaType)) {
						trimmedPreferences.add(preference);
						clientInfo.setAcceptedMediaTypes(trimmedPreferences);
					}
				}

			}
		}
		return CONTINUE;
	}

}
