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

import org.restlet.Request;
import org.restlet.data.Cookie;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.user.UserView.UserViewList;
import com.sixsq.slipstream.util.HtmlUtil;
import com.sixsq.slipstream.util.RequestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class UserListResource extends ServerResource {

	private Configuration configuration;

	private User user = null;

	@Override
	public void doInit() throws ResourceException {

		Request request = getRequest();

		Cookie cookie = CookieUtils.extractAuthnCookie(request);
		String username = CookieUtils.getCookieUsername(cookie);

		user = User.loadByName(username);

		configuration = RequestUtil.getConfigurationFromRequest(request);
	}

	@Get("txt")
	public Representation toTxt() {
		String viewList = serializedUserViewList(User.viewList());
		return new StringRepresentation(viewList);
	}

	@Get("xml")
	public Representation toXml() {
		String viewList = serializedUserViewList(User.viewList());
		return new StringRepresentation(viewList);
	}

	@Get("html")
	public Representation toHtml() {

		Request request = getRequest();
		String baseUrlSlash = RequestUtil.getBaseUrlSlash(request);
		UserViewList userViewList = new UserViewList(User.viewList());

		return HtmlUtil.transformToHtml(baseUrlSlash, "user",
				configuration.version, "user-list.xsl", user, userViewList);
	}

	private String serializedUserViewList(List<UserView> viewList) {
		UserViewList userViewList = new UserViewList(viewList);
		return SerializationUtil.toXmlString(userViewList);
	}

}
