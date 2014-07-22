package com.sixsq.slipstream.stats;

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

import java.util.Arrays;
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.metering.Metering;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.util.SerializationUtil;

public class StatsResource extends BaseResource {

	public static final String USER_QUERY_PARAMETER = "user";

	@Get("xml|html")
	public Representation toXml() {

		String metadata = SerializationUtil.toXmlString(compute());
		return new StringRepresentation(metadata, MediaType.APPLICATION_XML);

	}

	private Metering compute() {

		Metering measurements = new Metering();

		List<User> users = null;

		if (getUser().isSuper()) {
			User user = extractUserQuery();
			if(user != null) {
				users = Arrays.asList(user);
			} else {
				users = User.list();
			}
		} else {
			users = Arrays.asList(getUser());
		}

		for (User user : users) {

			try {
				measurements.populate(user);
			} catch (SlipStreamClientException e) {
				throwClientConflicError(e.getMessage());
			} catch (ConfigurationException e) {
				throwServerError(e);
			} catch (AbortException e) {
				throwServerError(e);
			}

		}
		return measurements;
	}

	private User extractUserQuery() {
		String username = null;
		User user = null;
		if (getRequest().getAttributes().containsKey(USER_QUERY_PARAMETER)) {
			username = (String) getRequest().getAttributes().get(
					USER_QUERY_PARAMETER);
			try {
				user = User.loadByName(username);
			} catch (ConfigurationException e) {
				throwConfigurationException(e);
			} catch (ValidationException e) {
				throwClientValidationError(e.getMessage());
			}
		}
		
		return user;
	}

	@Override
	protected String getPageRepresentation() {
		return "runs";
	}

}
