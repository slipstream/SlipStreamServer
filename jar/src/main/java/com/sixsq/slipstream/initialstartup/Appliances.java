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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import com.sixsq.slipstream.connector.ParametersFactory;
import com.sixsq.slipstream.connector.stratuslab.StratusLabConnector;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.Platforms;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Package;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.persistence.Target;
import com.sixsq.slipstream.persistence.User;

public class Appliances extends ModuleCreator {

	public static void create() throws ValidationException, NotFoundException,
			ConfigurationException, NoSuchAlgorithmException,
			UnsupportedEncodingException {

		if (ProjectModule.load(ProjectModule
				.constructResourceUri(APPLIANCES_PROJECT_NAME)) != null) {
			return;
		}

		createProjectModules();
		createRStudioAppliance();
		createTorqueMasterAppliance();
		createTorqueWorkerAppliance();
	}

	private static void createProjectModules() throws ValidationException,
			NotFoundException {

		User user = User.loadByName(SIXSQ);

		Module module;

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(PUBLIC_PROJECT_NAME))) {
			module = new ProjectModule(PUBLIC_PROJECT_NAME);
			Authz authz = createPublicGetAuthz(user, module);
			authz.setInheritedGroupMembers(false);
			authz.setPublicCreateChildren(true);
			module.setAuthz(authz);
			module.store();
		}

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(APPLIANCES_PROJECT_NAME))) {
			module = new ProjectModule(APPLIANCES_PROJECT_NAME);
			module.setAuthz(createPublicGetAuthz(user, module));
			module.store();
		}

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
					.constructResourceUri(CENTOS_IMAGE_NAME));
			rstudio.setPlatform(Platforms.redhat.toString());
			
			rstudio.setDescription("RStudio analysis server");
			
			rstudio.setLoginUser("root");

			String script = "#!/bin/bash -x\n"
					+ "\n"
					+ "#\n"
					+ "# install RStudio\n"
					+ "#\n"
					+ "wget http://download2.rstudio.org/rstudio-server-0.97.551-x86_64.rpm\n"
					+ "yum install -y --nogpgcheck rstudio-server-0.97.551-x86_64.rpm\n"
					+ "\n" + "#\n" + "# put this on standard port\n" + "#\n"
					+ "echo 'www-port=80' > /etc/rstudio/rserver.conf\n";

			rstudio.setRecipe(script);

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
					+ "adduser ruser --password $crypt_ruser_password\n"
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

			rstudio.getPackages().add(new Package("R"));

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

	private static void createTorqueMasterAppliance()
			throws ValidationException, NotFoundException {

		User user = User.loadByName(SIXSQ);

		if (!ProjectModule.exists(ProjectModule
				.constructResourceUri(TORQUE_MASTER_IMAGE_NAME))) {

			ImageModule tmaster = new ImageModule(TORQUE_MASTER_IMAGE_NAME);
			tmaster.setAuthz(createPublicGetAuthz(user, tmaster));

			tmaster.setIsBase(true);
			tmaster.getCloudImageIdentifiers().add(
					new CloudImageIdentifier(tmaster, new StratusLabConnector()
							.getCloudServiceName(),
							"H8dg0ssw_j4jg67FTwXysCUrJPl"));
			
			tmaster.setDescription("master node for torque cluster");

			tmaster.setPlatform(Platforms.redhat.toString());
			tmaster.setLoginUser("root");

			String script = "#!/bin/bash -x\n"
					+ "\n"
					+ "#\n"
					+ "# turn off firewall for now\n"
					+ "#\n"
					+ "service iptables stop\n"
					+ "\n"
					+ "#\n"
					+ "# create normal user\n"
					+ "#\n"
					+ "adduser --uid 1000 --user-group tuser\n"
					+ "\n"
					+ "#\n"
					+ "# install the torque batch cluster software for master\n"
					+ "#\n"
					+ "yum install -y torque-server torque-scheduler torque-client\n"
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
					+ "echo $master_hostname >  /var/lib/torque/server_priv/nodes\n"
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
					+ "cp /usr/share/doc/torque-2.5.7/torque.setup .\n"
					+ "chmod a+x torque.setup\n"
					+ "./torque.setup root $master_hostname\n"
					+ "\n"
					+ "#\n"
					+ "# now start the torque scheduler (simple FIFO)\n"
					+ "#\n"
					+ "service pbs_sched start \n"
					+ "\n"
					+ "#\n"
					+ "# pull in node names of all of the workers\n"
					+ "#\n"
					+ "for (( i=1; i <= `ss-get worker.1:multiplicity`; i++ )); do\n"
					+ "  worker_ip=`ss-get --timeout 480 worker.$i:hostname`\n"
					+ "  cmd=\"import socket; print socket.getfqdn('$worker_ip')\"\n"
					+ "  worker_hostname=`python -c \"$cmd\"`\n"
					+ "  echo $worker_hostname >> /var/lib/torque/server_priv/nodes\n"
					+ "done\n"
					+ "\n"
					+ "#\n"
					+ "# restart the server so that it will see all workers\n"
					+ "#\n"
					+ "service pbs_server restart\n"
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

			tworker.setIsBase(true);
			tworker.getCloudImageIdentifiers().add(
					new CloudImageIdentifier(tworker, new StratusLabConnector()
							.getCloudServiceName(),
							"H8dg0ssw_j4jg67FTwXysCUrJPl"));
			
			tworker.setDescription("worker node for torque cluster");

			tworker.setPlatform(Platforms.redhat.toString());
			tworker.setLoginUser("root");

			String script = "#!/bin/bash -x\n"
					+ "\n"
					+ "#\n"
					+ "# turn off firewall for now\n"
					+ "#\n"
					+ "service iptables stop\n"
					+ "\n"
					+ "#\n"
					+ "# create normal user\n"
					+ "#\n"
					+ "adduser --uid 1000 --user-group tuser\n"
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
					+ "echo \"\\$pbsserver \" $master_hostname > /etc/torque/mom/config\n"
					+ "\n"
					+ "#\n"
					+ "# start the worker daemon\n"
					+ "#\n"
					+ "service pbs_mom start \n"
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

	private static Authz createPublicGetAuthz(User user, Module module) {
		Authz authz = new Authz(user.getName(), module);
		authz.setPublicGet(true);
		return authz;
	}

}
