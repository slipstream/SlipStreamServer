package com.sixsq.slipstream.connector;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.run.RunView;

public class Collector {

	private static Logger logger = Logger.getLogger(Collector.class.getName());

	public static int collect(User user, Connector connector) {
		int res = 0;
		try {
			res = describeInstances(user, connector);
		} catch (ConfigurationException e) {
			logger.severe(e.getMessage());
		} catch (ValidationException e) {
			logger.warning(e.getMessage());
		}
		return res;
	}

	private static int describeInstances(User user, Connector connector)
			throws ConfigurationException, ValidationException {
		user.addSystemParametersIntoUser(Configuration.getInstance()
				.getParameters());
		Properties props = new Properties();
		try {
			props = connector.describeInstances(user);
		} catch (Exception e) {
			logger.warning("Failed contecting to cloud: "
					+ connector.getConnectorInstanceName() + " onbehalf of " + user);
			// swallow the exception, since we don't want to fail if users
			// have wrong credentials
		}
		return populateVmsForCloud(user, connector.getConnectorInstanceName(), props);
	}

	private static int populateVmsForCloud(User user, String cloud,
			Properties props) {
		List<Vm> vms = new ArrayList<Vm>();
		for (String key : props.stringPropertyNames()) {
			String instanceId = key;
			String state = (String) props.get(key);
			Vm vm = new Vm(instanceId, cloud, state, user.getName());
			try {
				String runUuid = fetchRunUuid(user, cloud, instanceId);
				vm.setRunUuid(runUuid);
			} catch (ConfigurationException e) {
				logger.warning("Error fetching run uuid for instance id "
						+ instanceId + " for cloud " + cloud);
			} catch (ValidationException e) {
				logger.warning("Error fetching run uuid for instance id "
						+ instanceId + " for cloud " + cloud);
			}
			vms.add(vm);
		}
		Vm.update(vms, user.getName(), cloud);
		return props.size();
	}

	private static String fetchRunUuid(User user, String cloud,
			String instanceId) throws ConfigurationException,
			ValidationException {
		RunView run = Run.loadViewByInstanceId(user, instanceId, cloud);
		if (run == null) {
			return "Unknown";
		}
		return run.uuid;
	}
}
