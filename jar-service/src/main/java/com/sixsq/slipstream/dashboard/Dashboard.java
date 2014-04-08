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
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.run.Quota;
import com.sixsq.slipstream.run.Runs;

@Root
public class Dashboard extends Runs {

	public static class UsageElement {
		@Attribute
		private String cloud;
		@Attribute
		private int quota;
		@Attribute
		private int currentUsage;

		public UsageElement() {
        }

		public UsageElement(String cloud, int quota, int currentUsage) {
			this.cloud = cloud;
			this.quota = quota;
			this.currentUsage = currentUsage;
		}

        public String getCloud() {
            return cloud;
        }

        public int getQuota() {
            return quota;
        }

        public int getCurrentUsage() {
            return currentUsage;
        }
    }

	@ElementList
	private transient List<UsageElement> usage = new ArrayList<UsageElement>();

	public void populate(User user) throws SlipStreamException {
		super.populate(user);

		user = User.loadByName(user.getName());  // ensure user is loaded from database

		Map<String, Integer> cloudUsage = Vm.usage(user.getName());
		for (String cloud : ConnectorFactory.getCloudServiceNamesList()) {
			Integer quota = Integer.parseInt(Quota.getValue(user, cloud));
			Integer currentUsage = cloudUsage.get(cloud);
			if (currentUsage == null) currentUsage = 0;

			usage.add(new UsageElement(cloud, quota, currentUsage));
		}
	}

}
