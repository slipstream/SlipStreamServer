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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.run.Quota;
import com.sixsq.slipstream.run.RunViewList;

@Root
public class Dashboard {

	public static class UsageElement {

		@Attribute
		private String cloud;

		@Attribute
		private int quota;

		@Attribute
		private int userUsage;

		@Attribute
		private int userInactiveUsage;

		@Attribute
		private int othersUsage;

		@Attribute
		private int pendingUsage;

		@Attribute
		private int unknownUsage;

		public UsageElement() {
        }

		public UsageElement(String cloud, int quota, int userUsage, int userInactiveUsage, int othersUsage,
							int pendingUsage, int unknownUsage)
		{
			this.cloud = cloud;
			this.quota = quota;
			this.userUsage = userUsage;
			this.userInactiveUsage = userInactiveUsage;
			this.othersUsage = othersUsage;
			this.unknownUsage = unknownUsage;
			this.pendingUsage = pendingUsage;
		}

        public String getCloud() {
            return cloud;
        }

        public int getQuota() {
            return quota;
        }
    }

	@ElementList
	private transient List<String> clouds = new ArrayList<String>();

	@ElementList
	private transient List<UsageElement> usages = new ArrayList<UsageElement>();

	public void populate(User user) throws SlipStreamException {

		user = User.loadByName(user.getName());  // ensure user is loaded from database

		Map<String, Map<String, Integer>> cloudUsage = Vm.usage(user.getName());
		clouds = ConnectorFactory.getCloudServiceNamesList();

		for (String cloud : clouds) {
			Integer quota = Integer.parseInt(Quota.getValue(user, cloud));
			if (cloudUsage.containsKey(cloud)) {
				Map<String, Integer> usage = cloudUsage.get(cloud);

				int userUsage = usage.getOrDefault("userUsage", 0);
				int userInactiveUsage = usage.getOrDefault("userInactiveUsage", 0);
				int othersUsage = usage.getOrDefault("othersUsage", 0);
				int pendingUsage = usage.getOrDefault("pendingUsage", 0);
				int unknownUsage = usage.getOrDefault("unknownUsage", 0);

				usages.add(new UsageElement(cloud, quota, userUsage, userInactiveUsage, othersUsage, pendingUsage,
						                    unknownUsage));
			}
		}
	}

}
