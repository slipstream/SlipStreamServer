package com.sixsq.slipstream.authz;

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

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Authorizer;
import org.restlet.security.User;

import com.sixsq.slipstream.persistence.Run;

public class ReportsAuthorizer extends Authorizer {

	@Override
	protected boolean authorize(Request request, Response response) {
		User user = request.getClientInfo().getUser();

		if(isSuperRole(request)) {
			return true;
		}
		
		String uuid = (String) request.getResourceRef().getRelativeRef().toString();
		uuid = uuid.split("/")[0];
		Run run = Run.loadFromUuid(uuid);
		if(run == null) {
			return false;
		}
		return run.getUser().equals(user.getName()) ? true : false;
	}

	private boolean isSuperRole(Request request) {
		return request.getClientInfo().getRoles().contains(SuperEnroler.SUPER);
	}

}
