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

import static com.sixsq.slipstream.initialstartup.Users.SIXSQ;

import com.sixsq.slipstream.connector.ParametersFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.Platforms;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.Package;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.persistence.Target;
import com.sixsq.slipstream.persistence.User;

public class Tutorials extends Images {

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

		createRStudioAppliance();
		createRStudioDeploymentModule();

		createTorqueMasterAppliance();
		createTorqueWorkerAppliance();
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

		module = new ProjectModule(SERVICE_TESTING_PROJECT_NAME);
		authz = new Authz(user.getName(), module);
		authz.setPublicGet(true);
		module.setAuthz(authz);
		module.store();

		module = new ProjectModule(RSTUDIO_PROJECT_NAME);
		authz = new Authz(user.getName(), module);
		authz.setPublicGet(true);
		module.setAuthz(authz);
		module.store();

		module = new ProjectModule(TORQUE_PROJECT_NAME);
		authz = new Authz(user.getName(), module);
		authz.setPublicGet(true);
		module.setAuthz(authz);
		module.store();

	}

	private static Module createWebServerModule() throws ValidationException,
			NotFoundException {

		ModuleParameter mp;
		Authz authz;

		ImageModule webserver = new ImageModule(APACHE_IMAGE_NAME);
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

	private static Module createTestClientModule() throws ValidationException,
			NotFoundException {

		ModuleParameter mp;
		Authz authz;

		ImageModule testclient = new ImageModule(CLIENT_IMAGE_NAME);
		testclient
				.setDescription("Test client testing correct connectivity and data content with web server");
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

		mp = new ModuleParameter("webserver.hostname", "", "Server hostname");
		mp.setCategory(ParameterCategory.Input);
		testclient.setParameter(mp);

		ParametersFactory.addParametersForEditing(testclient);

		testclient.store();

		return testclient;

	}

	private static void createClientServerDeploymentModule(Module webserver,
			Module client) throws ValidationException {
		DeploymentModule deployment = new DeploymentModule(
				SERVICE_TESTING_DEPLOYMENT_NAME);
		deployment
				.setDescription("Deployment binding the apache server and the test client nodes");
		Authz authz = new Authz(user.getName(), deployment);
		authz.setPublicGet(true);
		authz.setPublicPost(true);
		deployment.setAuthz(authz);
		NodeParameter np;

		Node apache = new Node("apache",
				ImageModule.constructResourceUri(webserver.getName()));

		deployment.getNodes().put(apache.getName(), apache);

		Node testclient = new Node("testclient",
				ImageModule.constructResourceUri(client.getName()));
		np = new NodeParameter("webserver.hostname", "apache:hostname");
		testclient.setParameterMapping(np);
		np = new NodeParameter("webserver.port", "apache:port");
		testclient.setParameterMapping(np);
		np = new NodeParameter("webserver.ready", "apache:ready");
		testclient.setParameterMapping(np);

		deployment.getNodes().put(testclient.getName(), testclient);

		deployment.store();
	}

	private static void createRStudioAppliance() throws ValidationException,
			NotFoundException {

		User user = User.loadByName(SIXSQ);

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(RSTUDIO_IMAGE_NAME))) {

			ImageModule rstudio = new ImageModule(RSTUDIO_IMAGE_NAME);
			rstudio.setAuthz(createPublicGetAuthz(user, rstudio));
			rstudio.setIsBase(false);
			rstudio.setModuleReference(Module
					.constructResourceUri(UBUNTU_IMAGE_NAME));
			rstudio.setPlatform(Platforms.ubuntu.toString());

			rstudio.setDescription("RStudio analysis server");

			rstudio.setLoginUser("root");

			String script = "#!/bin/bash -x\n"
					+ "\n"
					+ "#\n"
					+ "# install RStudio\n"
					+ "#\n"
					+ "wget http://download2.rstudio.org/rstudio-server-0.97.551-amd64.deb\n"
					+ "gdebi rstudio-server-0.97.551-amd64.deb\n" + "\n"
					+ "#\n" + "# put this on standard port\n" + "#\n"
					+ "echo 'www-port=80' > /etc/rstudio/rserver.conf\n";

			rstudio.setRecipe(script);

			rstudio.getPackages().add(new Package("r-base"));
			rstudio.getPackages().add(new Package("gdebi-core"));
			rstudio.getPackages().add(new Package("libapparmor1"));

			script = "#!/bin/bash -x\n"
					+ "\n"
					+ "#\n"
					+ "# create a random password for ruser\n"
					+ "#\n"
					+ "ruser_password=`openssl rand -base64 8`\n"
					+ "crypt_ruser_password=`echo $ruser_password | openssl passwd -crypt -stdin`\n"
					+ "\n"
					+ "#\n"
					+ "# create the ruser account with this password\n"
					+ "#\n"
					+ "adduser --quiet --disabled-password --gecos '' ruser\n"
					+ "usermod --password $crypt_ruser_password ruser\n"
					+ "\n"
					+ "#\n"
					+ "# publish password so user can log in\n"
					+ "# will be visible in machine parameters in SlipStream interface\n"
					+ "#\n"
					+ "ss-set rstudio_user ruser\n"
					+ "ss-set rstudio_pswd $ruser_password\n"
					+ "\n"
					+ "#\n"
					+ "# restart the server to ensure all changes are taken into account\n"
					+ "#\n"
					+ "rstudio-server restart \n"
					+ "\n"
					+ "#\n"
					+ "# set the customstate to inform user that everything's ready\n"
					+ "#\n" + "ss-set statecustom 'RStudio Ready!'\n";

			rstudio.getTargets().add(new Target(Target.EXECUTE_TARGET, script));

			ModuleParameter mp = new ModuleParameter("rstudio_pswd", "",
					"password for RStudio");
			mp.setCategory(ParameterCategory.Output);
			rstudio.setParameter(mp);

			mp = new ModuleParameter("rstudio_user", "", "username for RStudio");
			mp.setCategory(ParameterCategory.Output);
			rstudio.setParameter(mp);

			ParametersFactory.addParametersForEditing(rstudio);
			rstudio.store();
		}

	}

	private static void createRStudioDeploymentModule()
			throws ValidationException {

		DeploymentModule deployment = new DeploymentModule(
				RSTUDIO_DEPLOYMENT_NAME);
		deployment.setDescription("Standalone RStudio Server");
		Authz authz = new Authz(user.getName(), deployment);
		authz.setPublicGet(true);
		authz.setPublicPost(true);
		deployment.setAuthz(authz);

		Node rstudio = new Node("rstudio",
				ImageModule.constructResourceUri(RSTUDIO_IMAGE_NAME));

		deployment.getNodes().put(rstudio.getName(), rstudio);

		deployment.store();
	}

	private static void createTorqueMasterAppliance()
			throws ValidationException, NotFoundException {

		User user = User.loadByName(SIXSQ);

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(TORQUE_MASTER_IMAGE_NAME))) {

			ImageModule tmaster = new ImageModule(TORQUE_MASTER_IMAGE_NAME);
			tmaster.setAuthz(createPublicGetAuthz(user, tmaster));

			tmaster.setIsBase(false);
			tmaster.setModuleReference(Module
					.constructResourceUri(UBUNTU_IMAGE_NAME));
			tmaster.setPlatform(Platforms.ubuntu.toString());

			tmaster.setDescription("master node for torque cluster");

			tmaster.setPlatform(Platforms.ubuntu.toString());
			tmaster.setLoginUser("root");

			tmaster.getPackages().add(new Package("munge"));
			tmaster.getPackages().add(new Package("torque-scheduler"));
			tmaster.getPackages().add(new Package("torque-server"));
			tmaster.getPackages().add(new Package("torque-client"));

			String script = "#!/bin/bash -x\n"
					+ "\n"
					+ "#\n"
					+ "# create normal user\n"
					+ "#\n"
					+ "adduser --quiet --uid 1000 --disabled-password --gecos '' tuser\n"
					+ "\n"
					+ "#\n"
					+ "# master hostname (must be a name!)\n"
					+ "#\n"
					+ "master_ip=`ss-get --timeout 480 master.1:hostname`\n"
					+ "cmd=\"import socket; print socket.getfqdn('$master_ip')\"\n"
					+ "export master_hostname=`python -c \"$cmd\"`\n"
					+ "\n"
					+ "ss-set master_hostname $master_hostname\n"
					+ "\n"
					+ "#\n"
					+ "# initialize the server configuration with hostname\n"
					+ "# \n"
					+ "touch /var/spool/torque/server_priv/nodes\n"
					+ "echo $master_hostname >  /etc/torque/server_name \n"
					+ "\n"
					+ "#\n"
					+ "# configure and start munge service\n"
					+ "#\n"
					+ "create-munge-key\n"
					+ "service munge start \n"
					+ "\n"
					+ "#\n"
					+ "# set parameter with munge key to be shared with workers\n"
					+ "# value is the base64 encoded value of the binary file\n"
					+ "#\n"
					+ "ss-set munge_key64 `cat /etc/munge/munge.key | base64 --wrap 0`\n"
					+ "\n"
					+ "#\n"
					+ "# minimal setup of the batch system and queues\n"
					+ "#\n"
					+ "cp /usr/share/doc/torque-common/torque.setup .\n"
					+ "chmod a+x torque.setup\n"
					+ "./torque.setup root $master_hostname\n"
					+ "\n"
					+ "#\n"
					+ "# now start the torque scheduler (simple FIFO)\n"
					+ "#\n"
					+ "service torque-scheduler start\n"
					+ "\n"
					+ "#\n"
					+ "# pull in node names of all of the workers\n"
					+ "#\n"
					+ "for (( i=1; i <= `ss-get worker.1:multiplicity`; i++ )); do\n"
					+ "  worker_ip=`ss-get --timeout 480 worker.$i:hostname`\n"
					+ "  cmd=\"import socket; print socket.getfqdn('$worker_ip')\"\n"
					+ "  worker_hostname=`python -c \"$cmd\"`\n"
					+ "  echo $worker_hostname >> /var/spool/torque/server_priv/nodes\n"
					+ "done\n"
					+ "\n"
					+ "#\n"
					+ "# restart the server so that it will see all workers\n"
					+ "#\n"
					+ "service torque-server restart\n"
					+ "\n"
					+ "#\n"
					+ "# configuration for ssh access between nodes as user\n"
					+ "#\n"
					+ "su - tuser -c 'mkdir -p /home/tuser/.ssh'\n"
					+ "su - tuser -c 'chmod 0700 /home/tuser/.ssh'\n"
					+ "su - tuser -c 'ssh-keygen -f /home/tuser/.ssh/id_rsa -N \"\"'\n"
					+ "\n"
					+ "cp /root/.ssh/authorized_keys /home/tuser/.ssh/authorized_keys\n"
					+ "chown tuser:tuser /home/tuser/.ssh/authorized_keys\n"
					+ "chmod 0600 /home/tuser/.ssh/authorized_keys\n"
					+ "su - tuser -c 'cat /home/tuser/.ssh/id_rsa.pub >> /home/tuser/.ssh/authorized_keys'\n"
					+ "\n"
					+ "ss-set user_id_rsa64 `cat /home/tuser/.ssh/id_rsa | base64 --wrap 0`\n";

			tmaster.getTargets().add(new Target(Target.EXECUTE_TARGET, script));

			ModuleParameter mp = new ModuleParameter("munge_key64", "",
					"base64 encoded munge key");
			mp.setCategory(ParameterCategory.Output);
			tmaster.setParameter(mp);

			mp = new ModuleParameter("user_id_rsa64", "",
					"base64 encoded ssh private key for user");
			mp.setCategory(ParameterCategory.Output);
			tmaster.setParameter(mp);

			mp = new ModuleParameter("master_hostname", "",
					"hostname as name, not IP address");
			mp.setCategory(ParameterCategory.Output);
			tmaster.setParameter(mp);

			ParametersFactory.addParametersForEditing(tmaster);
			tmaster.store();
		}

	}

	private static void createTorqueWorkerAppliance()
			throws ValidationException, NotFoundException {

		User user = User.loadByName(SIXSQ);

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(TORQUE_WORKER_IMAGE_NAME))) {

			ImageModule tworker = new ImageModule(TORQUE_WORKER_IMAGE_NAME);
			tworker.setAuthz(createPublicGetAuthz(user, tworker));

			tworker.setIsBase(false);
			tworker.setModuleReference(Module
					.constructResourceUri(UBUNTU_IMAGE_NAME));
			tworker.setPlatform(Platforms.ubuntu.toString());

			tworker.setDescription("worker node for torque cluster");

			tworker.setPlatform(Platforms.ubuntu.toString());
			tworker.setLoginUser("root");

			tworker.getPackages().add(new Package("munge"));
			tworker.getPackages().add(new Package("torque-mom"));

			String script = "#!/bin/bash -x\n"
					+ "\n"
					+ "#\n"
					+ "# create normal user\n"
					+ "#\n"
					+ "adduser --quiet --uid 1000 --disabled-password --gecos '' tuser\n"
					+ "\n"
					+ "#\n"
					+ "# install the torque batch worker daemon\n"
					+ "#\n"
					+ "yum install -y torque-mom\n"
					+ "\n"
					+ "#\n"
					+ "# import the munge key from the server and start daemon\n"
					+ "#\n"
					+ "export munge_key64=`ss-get --timeout 480 master.1:munge_key64`\n"
					+ "echo $munge_key64 | base64 -d > /etc/munge/munge.key\n"
					+ "chown munge:munge /etc/munge/munge.key\n"
					+ "chmod 0400 /etc/munge/munge.key\n"
					+ "service munge start\n"
					+ "\n"
					+ "#\n"
					+ "# setup the worker daemon with master's hostname\n"
					+ "#\n"
					+ "master_ip=`ss-get master.1:hostname`\n"
					+ "cmd=\"import socket; print socket.getfqdn('$master_ip')\"\n"
					+ "export master_hostname=`python -c \"$cmd\"`\n"
					+ "\n"
					+ "echo \"\\$pbsserver \" $master_hostname > /var/spool/torque/mom_priv/config\n"
					+ "\n"
					+ "#\n"
					+ "# start the worker daemon\n"
					+ "#\n"
					+ "service torque-mom start \n"
					+ "\n"
					+ "#\n"
					+ "# setup SSH configuration\n"
					+ "#\n"
					+ "su - tuser -c 'mkdir -p /home/tuser/.ssh'\n"
					+ "ss-get --timeout 480 master.1:user_id_rsa64 | base64 -d >> /home/tuser/.ssh/id_rsa\n"
					+ "chown tuser:tuser /home/tuser/.ssh/id_rsa\n"
					+ "chmod 0400 /home/tuser/.ssh/id_rsa\n"
					+ "\n"
					+ "ssh-keyscan `ss-get master.1:hostname` >> /home/tuser/.ssh/known_hosts\n"
					+ "ssh-keyscan `ss-get master.1:master_hostname` >> /home/tuser/.ssh/known_hosts\n";

			tworker.getTargets().add(new Target(Target.EXECUTE_TARGET, script));

			ParametersFactory.addParametersForEditing(tworker);
			tworker.store();

		}

	}

	private static void createTorqueDeploymentModule()
			throws ValidationException {

		DeploymentModule deployment = new DeploymentModule(
				TORQUE_DEPLOYMENT_NAME);
		deployment.setDescription("Torque Batch Cluster");
		Authz authz = new Authz(user.getName(), deployment);
		authz.setPublicGet(true);
		authz.setPublicPost(true);
		deployment.setAuthz(authz);

		Node tmaster = new Node("master",
				ImageModule.constructResourceUri(TORQUE_MASTER_IMAGE_NAME));
		deployment.getNodes().put(tmaster.getName(), tmaster);

		Node tworker = new Node("worker",
				ImageModule.constructResourceUri(TORQUE_WORKER_IMAGE_NAME));
		tworker.setMultiplicity(2);
		deployment.getNodes().put(tworker.getName(), tworker);

		deployment.store();
	}

}
