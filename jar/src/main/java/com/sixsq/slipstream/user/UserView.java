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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.persistence.User;

@Root(name = "item")
public class UserView {

	@Attribute
	public final String name;

	@Attribute
	public final String resourceUri;

	@Attribute(required=false)
	public final String firstName;

	@Attribute(required=false)
	public final String lastName;

	@Attribute(required=false)
	public final User.State state;

	public UserView(String name, String firstName,
			String lastName, User.State state) {

		this.name = name;
		this.resourceUri = User.constructResourceUri(name);

		this.firstName = firstName;
		this.lastName = lastName;

		this.state = state;

	}

	@Root(name = "list")
	public static class UserViewList {

		@SuppressWarnings("unused")
		@ElementList(inline = true, required = false)
		private final List<UserView> list;

		public UserViewList(List<UserView> list) {
			this.list = list;
		}
	}

}
