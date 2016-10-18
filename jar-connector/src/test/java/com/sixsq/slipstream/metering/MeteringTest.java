package com.sixsq.slipstream.metering;

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


import com.sixsq.slipstream.connector.Collector;
import com.sixsq.slipstream.connector.ConnectorTestBase;
import com.sixsq.slipstream.connector.UsageRecorder;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.run.RunTestBase;
import com.sixsq.slipstream.util.CommonTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class MeteringTest extends RunTestBase {

	private static String CLOUD_A = "local";
	private static String CLOUD_B = "cloudB";
	private static String CLOUD_C = "cloudC";
	private static String RUNNING_VM_STATE = "Running";


	@BeforeClass
	public static void setupClass() throws ConfigurationException, SlipStreamException {
		UsageRecorder.muteForTests();
		ConnectorTestBase.setupElasticseach();
		createUser();
		String username = user.getName();

		CommonTestUtil.setCloudConnector(CLOUD_A + ":local," +
										 CLOUD_B + ":local," +
										 CLOUD_C + ":local");

		String runId = "xxx";

		List<Vm> vms = new ArrayList<Vm>();

		vms.add(createVm("id_1", CLOUD_A, RUNNING_VM_STATE, username, runId));
		vms.add(createVm("id_2", CLOUD_A, RUNNING_VM_STATE, username, runId));
		vms.add(createVm("id_3", CLOUD_A, "Terminated", username, runId));
		Collector.update(vms, username, CLOUD_A);

		vms.clear();
		vms.add(createVm("id_1", CLOUD_B, "Pending", username, runId));
		vms.add(createVm("id_2", CLOUD_B, RUNNING_VM_STATE, username, runId));
		vms.add(createVm("id_3", CLOUD_B, "Terminated", username, runId));
		Collector.update(vms, username, CLOUD_B);

	}

	private static Vm createVm(String instanceid, String cloud, String state, String user, String runId) {
		Vm vm = new Vm(instanceid, cloud, state, user, new LocalConnector().isVmUsable(state), null, null, null, null);
		vm.setRunUuid(runId);
		vm.setRunOwner(user);
		return vm;
	}

	@Test
	public void cloudUsageData() throws SlipStreamException {

		Map<String, Integer> data = Metering.produceCloudUsageData(user.getName());

		assertThat(2, is(data.get(CLOUD_A)));
		assertThat(1, is(data.get(CLOUD_B)));
		assertThat(0, is(data.get(CLOUD_C)));
	}

	@Test
	public void cloudUsageDataForGraphite() throws SlipStreamException {

		Map<String, Integer> usageData = Metering.produceCloudUsageData(user.getName());
		String graphiteData = Metering.transformUsageDataForGraphite(user.getName(), usageData);

		String cloudAdata = getCloudDataForGraphite(CLOUD_A, "2");
		String cloudBdata = getCloudDataForGraphite(CLOUD_B, "1");
		String cloudCdata = getCloudDataForGraphite(CLOUD_C, "0");

		assertTrue(graphiteData.contains(cloudAdata));
		assertTrue(graphiteData.contains(cloudBdata));
		assertTrue(graphiteData.contains(cloudCdata));
		assertTrue(graphiteData.endsWith("\n"));
	}


	private String getCloudDataForGraphite(String cloudServiceName, String numberOfInstances) {
		return "slipstream." + user.getName() + ".usage.instance." + cloudServiceName + " " + numberOfInstances;
	}

}
