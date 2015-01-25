package com.sixsq.slipstream.user;

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

import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.resource.BaseResource;
import com.sixsq.slipstream.user.UserView.UserViewList;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class UserListResource extends BaseResource {

	@Get("csv")
	public Representation toTxt() {
		String result = "firstname, lastname, email, organisation, activesince, lastonine, lastexecute\n";
		for (UserView u : User.viewList()) {
			result += String.format("%s, %s, %s, %s, %s, %s, %s\n", u.firstName, u.lastName, u.email, u.organization,
					u.activeSince, u.lastOnline, u.lastExecute);
		}
		StringRepresentation response = new StringRepresentation(result);
		response.setMediaType(MediaType.TEXT_CSV);
		return response;
	}

	@Get("xml")
	public Representation toXml() {
		String viewList = serializedUserViewList(User.viewList());
		return new StringRepresentation(viewList, MediaType.APPLICATION_XML);
	}

	private String serializedUserViewList(List<UserView> viewList) {
		UserViewList userViewList = new UserViewList(viewList);
		return SerializationUtil.toXmlString(userViewList);
	}

	@Get("html")
	public Representation toHtml() {

		UserViewList userViewList = new UserViewList(User.viewList());

		String html = HtmlUtil.toHtml(userViewList, getPageRepresentation(), getUser(), getRequest());

		return new StringRepresentation(html, MediaType.TEXT_HTML);
	}

	protected String getPageRepresentation() {
		return "users";
	}

}
