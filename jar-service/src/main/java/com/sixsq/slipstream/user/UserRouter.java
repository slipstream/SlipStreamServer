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

import org.restlet.Context;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;
import org.restlet.security.Role;
import org.restlet.security.RoleAuthorizer;

import com.sixsq.slipstream.authz.SuperEnroler;

/**
 * User application to view and edit user data.
 * 
 * The URL layout of this router is the following:
 * 
 * <ul>
 * <li>/{user}?edit={edit}</li>
 * <li>/{user}</li>
 * <li>/</li>
 * </ul>
 * 
 * @author Marc-Elian Begin
 * 
 */
public class UserRouter extends Router {

	public UserRouter(Context context) {
		super(context);

		attachPaths(new String[] { "/{" + UserResource.USERNAME_URI_ATTRIBUTE + "}" }, UserResource.class);

		attachPathsWithSuper(new String[] { "" }, UserListResource.class);
	}

	private void attachPaths(String[] paths,
			Class<? extends ServerResource> resourceClass) {
		attachPathsWithRole(paths, resourceClass, null);
	}

	private void attachPathsWithSuper(String[] paths,
			Class<? extends ServerResource> resourceClass) {
		attachPathsWithRole(paths, resourceClass, SuperEnroler.Super);
	}

	private void attachPathsWithRole(String[] paths,
			Class<? extends ServerResource> c, Role role) {
		for (String path : paths) {
			attachPath(path, c, role);
		}
	}

	private void attachPath(String path,
			Class<? extends ServerResource> resourceClass, Role role) {
		RoleAuthorizer ra = new RoleAuthorizer();
		TemplateRoute route;
		if (role != null) {
			ra.getAuthorizedRoles().add(role);
			ra.setNext(resourceClass);
			route = attach(path, ra);
		} else {
			route = attach(path, resourceClass);
		}
		route.setMatchingMode(Template.MODE_STARTS_WITH);
	}
}
