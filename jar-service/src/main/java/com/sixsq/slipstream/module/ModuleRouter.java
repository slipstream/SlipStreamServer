package com.sixsq.slipstream.module;

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
import org.restlet.routing.Router;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.Variable;

import com.sixsq.slipstream.persistence.CloudImageIdentifier;

public class ModuleRouter extends Router {

	public ModuleRouter(Context context) {
		super(context);

		attachModulePublisher();

		attachSpecificVersion();

		attacheVersionList();

		attachModule();

		attachRootModule();

	}

	private void attachRootModule() {
		TemplateRoute route;
		route = attach("?chooser={chooser}", ModuleListResource.class);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables()
				.put("chooser", new Variable(Variable.TYPE_URI_QUERY));

		attach("", ModuleListResource.class);
	}

	private void attachModule() {
		TemplateRoute route;
		route = attach("/{module}?chooser={chooser}", ModuleResource.class);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_PATH));
		route.getTemplate().getVariables()
				.put("chooser", new Variable(Variable.TYPE_URI_QUERY));

		route = attach("/{module}?category={category}", ModuleResource.class);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_PATH));
		route.getTemplate().getVariables()
		.put("category", new Variable(Variable.TYPE_URI_QUERY));

		route = attach("/{module}?new={new}", ModuleResource.class);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables()
		.put("module", new Variable(Variable.TYPE_URI_PATH));
		route.getTemplate().getVariables()
		.put("new", new Variable(Variable.TYPE_URI_QUERY));

		route = attach("/{module}", ModuleResource.class);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_FRAGMENT));
	}

	private void attachSpecificVersion() {
		TemplateRoute route;

		route = attach("/{module}/{version}/{cloudservice}"
				+ CloudImageIdentifier.CLOUD_SERVICE_ID_SEPARATOR + "{region}",
				CloudResourceIdentifierResource.class);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_PATH));
		route.getTemplate().getVariables()
				.put("version", new Variable(Variable.TYPE_DIGIT));
		route.getTemplate().getVariables()
				.put("cloudmachineid", new Variable(Variable.TYPE_ALPHA));
		route.getTemplate().getVariables()
				.put("region", new Variable(Variable.TYPE_ALPHA));

		route = attach("/{module}/{version}/{cloudservice}",
				CloudResourceIdentifierResource.class);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_PATH));
		route.getTemplate().getVariables()
				.put("version", new Variable(Variable.TYPE_DIGIT));
		route.getTemplate().getVariables()
				.put("cloudmachineid", new Variable(Variable.TYPE_ALPHA));

		route = attach("/{module}/{version}?chooser={chooser}",
				ModuleResource.class);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_PATH));
		route.getTemplate().getVariables()
				.put("version", new Variable(Variable.TYPE_DIGIT));
		route.getTemplate().getVariables()
				.put("chooser", new Variable(Variable.TYPE_URI_QUERY));

		route = attach("/{module}/{version}", ModuleResource.class);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_PATH));
		route.getTemplate().getVariables()
				.put("version", new Variable(Variable.TYPE_DIGIT));
	}

	private void attacheVersionList() {
		TemplateRoute route;
		route = attach("/{module}/?chooser={chooser}",
				ModuleVersionListResource.class);
		route.setMatchingQuery(true);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_PATH));
		route.getTemplate().getVariables()
				.put("chooser", new Variable(Variable.TYPE_URI_QUERY));

		route = attach("/{module}/", ModuleVersionListResource.class);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_FRAGMENT));
	}

	private void attachModulePublisher() {
		TemplateRoute route;
		route = attach("/{module}/{version}/publish",
				ModulePublishResource.class);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_FRAGMENT));
		route.getTemplate().getVariables()
				.put("version", new Variable(Variable.TYPE_DIGIT));
		route = attach("/{module}/publish",
				ModulePublishResource.class);
		route.getTemplate().getVariables()
				.put("module", new Variable(Variable.TYPE_URI_FRAGMENT));
	}

}
