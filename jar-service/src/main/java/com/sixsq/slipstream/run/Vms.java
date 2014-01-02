package com.sixsq.slipstream.run;

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
import java.util.Properties;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import com.sixsq.slipsteam.run.RunView;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.run.VmView.VmViewList;

@Root
public class Vms {

	@Element
	private VmViewList vms = new VmViewList();

	public VmViewList getVms() {
		return vms;
	}

	public void populate(User user) throws SlipStreamException {
		user.validate();
		User.validateMinimumInfo(user);
		populateVms(user);
	}

	private void populateVms(User user) throws SlipStreamException {
		for (String cloudServiceName : ConnectorFactory.getCloudServiceNames()) {
			Connector connector = ConnectorFactory
					.getConnector(cloudServiceName);
			Properties props = new Properties();
			try {
				props = connector.describeInstances(user);
			} catch (Exception e) {
				// swallow the exception, since we don't want to fail if users
				// have wrong credentials
			}
			populateVmsForCloud(user, cloudServiceName, props);
		}
	}

	private void populateVmsForCloud(User user, String cloudServiceName,
			Properties props) throws ConfigurationException,
			ValidationException {
		for (String key : props.stringPropertyNames()) {
			String instanceId = key;
			String status = (String) props.get(key);
			String runUuid = fetchRunUuid(user, instanceId);
			vms.getList().add(
					new VmView(instanceId, status, runUuid, cloudServiceName));
		}
	}

	private String fetchRunUuid(User user, String instanceId)
			throws ConfigurationException, ValidationException {
		List<RunView> runs = Run.viewListByInstanceId(user, instanceId);
		if (runs.size() == 0) {
			return "Unknown";
		}
		return runs.get(0).uuid;
	}
}
