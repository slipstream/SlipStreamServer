package com.sixsq.slipstream.initialstartup;

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

import com.sixsq.slipstream.connector.ParametersFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.persistence.Target;
import com.sixsq.slipstream.persistence.User;

public class Tutorials extends BaseImages {

	private static final String TUTORIALS_PROJECT_NAME = PUBLIC_PROJECT_NAME
			+ "/Tutorials";
	private static final String CLIENT_SERVER_TUTORIALS_PROJECT_NAME = TUTORIALS_PROJECT_NAME
			+ "/HelloWorld";
	private static final String WEBSERVER_NAME = CLIENT_SERVER_TUTORIALS_PROJECT_NAME
			+ "/apache";
	private static final String TESTCLIENT_NAME = CLIENT_SERVER_TUTORIALS_PROJECT_NAME
			+ "/testclient";
	private static final String DEPLOYMENT_NAME = CLIENT_SERVER_TUTORIALS_PROJECT_NAME
			+ "/client_server";
	private static final String RSTUDIO_DEPLOYMENT_NAME = TUTORIALS_PROJECT_NAME
			+ "/rstudio";
	private static final String TORQUE_DEPLOYMENT_NAME = TUTORIALS_PROJECT_NAME
			+ "/torque";
	private static User user;

	public static void create() throws ValidationException, NotFoundException,
			ConfigurationException {

		if (ProjectModule.load(ProjectModule
				.constructResourceUri(TUTORIALS_PROJECT_NAME)) != null) {
			return;
		}

		Tutorials.createModules();
	}

	private static void createModules() throws ValidationException,
			NotFoundException {

		user = User.loadByName("sixsq");

		createProjects();
		Module webserver = createWebServerModule();
		Module client = createTestClientModule();
		createClientServerDeploymentModule(webserver, client);
		createRStudioDeploymentModule();
		createTorqueDeploymentModule();
	}

	private static void createProjects() throws ValidationException,
			NotFoundException {

		Module module;
		Authz authz;
		
		module = new ProjectModule(TUTORIALS_PROJECT_NAME);
		authz = new Authz(user.getName(), module);
		authz.setPublicGet(true);
		module.setAuthz(authz);
		module.store();

		module = new ProjectModule(CLIENT_SERVER_TUTORIALS_PROJECT_NAME);
		authz = new Authz(user.getName(), module);
		authz.setPublicGet(true);
		module.setAuthz(authz);
		module.store();

	}

	private static Module createWebServerModule()
			throws ValidationException, NotFoundException {

		ModuleParameter mp;
		Authz authz;
		
		ImageModule webserver = new ImageModule(WEBSERVER_NAME);
		webserver.setDescription("Apache web server");
		authz = new Authz(user.getName(), webserver);
		authz.setPublicGet(true);
		webserver.setAuthz(authz);
		webserver.setModuleReference(Module
				.constructResourceUri(UBUNTU_IMAGE_NAME));

		String executeTarget = "#!/bin/sh -xe\n"
				+ "apt-get update -y\n"
				+ "apt-get install -y apache2\n"
				+ "\n"
				+ "echo 'Hello from Apache deployed by SlipStream!' > /var/www/data.txt\n"
				+ "\n"
				+ "service apache2 stop\n"
				+ "port=$(ss-get port)\n"
				+ "sed -i -e 's/^Listen.*$/Listen '$port'/' /etc/apache2/ports.conf\n"
				+ "sed -i -e 's/^NameVirtualHost.*$/NameVirtualHost *:'$port'/' /etc/apache2/ports.conf\n"
				+ "sed -i -e 's/^<VirtualHost.*$/<VirtualHost *:'$port'>/' /etc/apache2/sites-available/default\n"
				+ "service apache2 start\n" + "ss-set ready true";

		webserver.getTargets().add(
				new Target(Target.EXECUTE_TARGET, executeTarget));

		String reportTarget = "#!/bin/sh -x\n"
			    + "cp /var/log/apache2/access.log $SLIPSTREAM_REPORT_DIR\n"
			    + "cp /var/log/apache2/error.log $SLIPSTREAM_REPORT_DIR";

		webserver.getTargets().add(
				new Target(Target.REPORT_TARGET, reportTarget));

		mp = new ModuleParameter("port", "8080", "Port");
		mp.setCategory(ParameterCategory.Output);
		webserver.setParameter(mp);

		mp = new ModuleParameter("ready", "",
				"Server ready to recieve connections");
		mp.setCategory(ParameterCategory.Output);
		webserver.setParameter(mp);

		ParametersFactory.addParametersForEditing(webserver);

		webserver.store();

		return webserver;

	}

	private static Module createTestClientModule()
			throws ValidationException, NotFoundException {

		ModuleParameter mp;
		Authz authz;

		ImageModule testclient = new ImageModule(TESTCLIENT_NAME);
		testclient.setDescription("Test client testing correct connectivity and data content with web server");
		authz = new Authz(user.getName(), testclient);
		authz.setPublicGet(true);
		testclient.setAuthz(authz);
		testclient.setModuleReference(Module
				.constructResourceUri(UBUNTU_IMAGE_NAME));

		String executeTarget = "#!/bin/sh -xe\n"
				+ "# Wait for the metadata to be resolved\n"
				+ "web_server_ip=$(ss-get --timeout 360 webserver.hostname)\n"
				+ "web_server_port=$(ss-get --timeout 360 webserver.port)\n"
				+ "ss-get --timeout 360 webserver.ready\n"
				+ "\n"
				+ "# Execute the test\n"
				+ "ENDPOINT=http://${web_server_ip}:${web_server_port}/data.txt\n"
				+ "wget -t 2 -O /tmp/data.txt ${ENDPOINT}\n"
				+ "[ \"$?\" = \"0\" ] & ss-set statecustom \"OK: $(cat /tmp/data.txt)\" || ss-abort \"Could not get the test file: ${ENDPOINT}\"\n";

		testclient.getTargets().add(
				new Target(Target.EXECUTE_TARGET, executeTarget));

		String reportTarget = "#!/bin/sh -x\n"
			+ "cp /tmp/data.txt $SLIPSTREAM_REPORT_DIR";

		testclient.getTargets().add(
			new Target(Target.REPORT_TARGET, reportTarget));

		mp = new ModuleParameter("webserver.port", "",
				"Port on which the web server listens");
		mp.setCategory(ParameterCategory.Input);
		testclient.setParameter(mp);

		mp = new ModuleParameter("webserver.ready", "",
				"Server ready to recieve connections");
		mp.setCategory(ParameterCategory.Input);
		testclient.setParameter(mp);

		mp = new ModuleParameter("webserver.hostname", "",
				"Server hostname");
		mp.setCategory(ParameterCategory.Input);
		testclient.setParameter(mp);
		
		ParametersFactory.addParametersForEditing(testclient);

		testclient.store();

		return testclient;

	}

	private static void createClientServerDeploymentModule(
			Module webserver, Module client)
			throws ValidationException {
		DeploymentModule deployment = new DeploymentModule(DEPLOYMENT_NAME);
		deployment.setDescription("Deployment binding the apache server and the test client nodes");
		Authz authz = new Authz(user.getName(), deployment);
		authz.setPublicGet(true);
		authz.setPublicPost(true);
		deployment.setAuthz(authz);
		NodeParameter np;

		Node apache = new Node("apache", ImageModule.constructResourceUri(webserver.getName()));

		deployment.getNodes().put(apache.getName(), apache);

		Node testclient = new Node("testclient", ImageModule.constructResourceUri(client.getName()));
		np = new NodeParameter("webserver.hostname", "apache:hostname");
		testclient.setParameterMapping(np);
		np = new NodeParameter("webserver.port", "apache:port");
		testclient.setParameterMapping(np);
		np = new NodeParameter("webserver.ready", "apache:ready");
		testclient.setParameterMapping(np);

		deployment.getNodes().put(testclient.getName(), testclient);

		deployment.store();
	}

	private static void createRStudioDeploymentModule()
			throws ValidationException {
		
		DeploymentModule deployment = new DeploymentModule(RSTUDIO_DEPLOYMENT_NAME);
		deployment.setDescription("Standalone RStudio Server");
		Authz authz = new Authz(user.getName(), deployment);
		authz.setPublicGet(true);
		authz.setPublicPost(true);
		deployment.setAuthz(authz);

		Node rstudio = new Node("rstudio", ImageModule.constructResourceUri("appliances/rstudio"));

		deployment.getNodes().put(rstudio.getName(), rstudio);

		deployment.store();
	}

	private static void createTorqueDeploymentModule()
			throws ValidationException {

		DeploymentModule deployment = new DeploymentModule(TORQUE_DEPLOYMENT_NAME);
		deployment.setDescription("Standalone RStudio Server");
		Authz authz = new Authz(user.getName(), deployment);
		authz.setPublicGet(true);
		authz.setPublicPost(true);
		deployment.setAuthz(authz);

		Node tmaster = new Node("rstudio", ImageModule.constructResourceUri("appliances/torque-master"));
		deployment.getNodes().put(tmaster.getName(), tmaster);

		Node tworker = new Node("rstudio", ImageModule.constructResourceUri("appliances/torque-worker"));
		deployment.getNodes().put(tworker.getName(), tworker);

		deployment.store();
	}

}
