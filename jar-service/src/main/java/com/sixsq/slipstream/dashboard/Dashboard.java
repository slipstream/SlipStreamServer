package com.sixsq.slipstream.dashboard;

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
import java.util.Map;

import com.sixsq.slipstream.exceptions.ValidationException;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.persistence.CloudUsage;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.run.Quota;

@Root
public class Dashboard {

	@ElementList
	private transient List<String> clouds = new ArrayList<String>();

	@ElementList
	private transient List<CloudUsage> usages = new ArrayList<CloudUsage>();

	private Integer getQuota(User user, String cloud) throws ValidationException {
		return Integer.parseInt(Quota.getValue(user, cloud));
	}

	public void populate(User user) throws SlipStreamException {

		user = User.loadByName(user.getName());  // ensure user is loaded from database

		clouds = ConnectorFactory.getCloudServiceNamesList();
		Map<String, CloudUsage> cloudUsages = Vm.usage(user.getName());

		CloudUsage allClouds = new CloudUsage("All Clouds");

		for (String cloud : clouds) {
			CloudUsage usage = cloudUsages.containsKey(cloud) ? cloudUsages.get(cloud) : new CloudUsage(cloud);

			usage.setQuota(getQuota(user, cloud));

			allClouds.add(usage);

			usages.add(usage);
		}

		usages.add(allClouds);
	}

}
