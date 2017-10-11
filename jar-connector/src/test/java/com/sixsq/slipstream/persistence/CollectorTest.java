package com.sixsq.slipstream.persistence;

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
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.UsageRecorder;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.SscljProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CollectorTest {

	private static Connector connector = null;
	private static String firstCloud = "firstCloud";
	private static String secondCloud = "secondCloud";
	private static String username = "user";
	private static User user = null;

	@Before
	public void setup() throws ValidationException {
		UsageRecorder.muteForTests();
		Event.muteForTests();
		SscljProxy.muteForTests();
		connector = new LocalConnector("localCloud");

		user = new User(username);

		for(Vm vm : Vm.list(user)) {
			vm.remove();
		}
		assertThat(Vm.list(user).size(), is(0));
	}

	@After
	public void tearDown() {
		Collector.update(new ArrayList<Vm>(), username, firstCloud);
		Collector.update(new ArrayList<Vm>(), username, secondCloud);
	}

	@Test
	public void update() throws ConfigurationException, ValidationException {

		// Insert in one cloud

		Vm vm = new Vm("fistCloudInstance1", firstCloud, "state", username, false);
		List<Vm> firstCloudVms = new ArrayList<Vm>();
		firstCloudVms.add(vm);
		Collector.update(firstCloudVms, username, firstCloud);
		firstCloudVms = Vm.list(user);
		assertThat(firstCloudVms.size(), is(1));

		// Insert one in another cloud

		vm = new Vm("secondCloudInstance1", secondCloud, "state", username, false);
		List<Vm> secondCloudVms = new ArrayList<Vm>();
		secondCloudVms.add(vm);
		Collector.update(secondCloudVms, username, secondCloud);
		assertThat(Vm.list(user).size(), is(2));

		// Insert a second in the first cloud

		vm = new Vm("fistCloudInstance2", firstCloud, "state", username, true);
		firstCloudVms.add(vm);
		Collector.update(firstCloudVms, username, firstCloud);
		assertThat(Vm.list(user).size(), is(3));

		// Replace first cloud vms
		vm = new Vm("fistCloudInstance3", firstCloud, "state", username, true);
		firstCloudVms = new ArrayList<Vm>();
		firstCloudVms.add(vm);

		Collector.update(firstCloudVms, username, firstCloud);

		List<Vm> allVms = Vm.list(user);

		// Here we cheat, mixing clouds
		Map<String, Vm> allVmsMap = Vm.toMapByInstanceId(allVms);
		assertThat(allVms.size(), is(2));
		assertThat(allVmsMap.get("fistCloudInstance3").getInstanceId(), is("fistCloudInstance3"));
		assertThat(allVmsMap.get("fistCloudInstance3").getCloud(), is(firstCloud));
		assertThat(allVmsMap.get("secondCloudInstance1").getInstanceId(), is("secondCloudInstance1"));
		assertThat(allVmsMap.get("secondCloudInstance1").getCloud(), is(secondCloud));

	}

	@Test
	public void usage() {
		Vm vm;
		String vmState;
		List<Vm> vms = new ArrayList<Vm>();

		String user = "user1";

		// cloud1
		vmState = "running";
		vm = new Vm("instance1", firstCloud, vmState, user, connector.isVmUsable(vmState));
		vm.setRunUuid("e06cf0dd-5266-472d-90b4-2a1a27af9dfa");
		vm.setRunOwner(user);
		vms.add(vm);

		vmState = "Running";
		vm = new Vm("instance2", firstCloud, vmState, user, connector.isVmUsable(vmState));
		vm.setRunUuid("10ac7940-6151-4d0f-b90c-0213b094bcd0");
		vm.setRunOwner(user);
		vms.add(vm);

		vmState = "terminated";
		vm = new Vm("instance3", firstCloud, vmState, user, connector.isVmUsable(vmState));
		vm.setRunUuid("abf3a90f-c024-411b-8536-164b4617c636");
		vm.setRunOwner(user);
		vms.add(vm);

		vmState = "running";
		vm = new Vm("instance5", firstCloud, vmState, user, connector.isVmUsable(vmState));
		vm.setRunUuid("Unknown");
		vms.add(vm);

		Collector.update(vms, "user1", firstCloud);

		// cloud2
		vms.clear();
		vmState = "on";
		vm = new Vm("instance4", secondCloud, vmState, user, connector.isVmUsable(vmState));
		vm.setRunUuid("8e519c11-e46b-43d0-a370-c738655e1c06");
		vm.setRunOwner(user);
		vms.add(vm);

		Collector.update(vms, "user1", secondCloud);

		// actual tests
		Map<String, CloudUsage> usage = Vm.usage(user);
		assertThat(usage.size(), is(2));
		assertThat(usage.get(firstCloud).getUserVmUsage(), is(2));
		assertThat(usage.get(secondCloud).getUserVmUsage(), is(1));
	}

	@Test
	public void updateState() throws ConfigurationException, ValidationException {
		String vmState;

		// Insert in one cloud

		vmState = "state";
		Vm vm = new Vm("instance1", firstCloud, vmState, username, connector.isVmUsable(vmState));
		List<Vm> vms = new ArrayList<Vm>();
		vms.add(vm);
		Collector.update(vms, username, firstCloud);
		vms = Vm.list(user);
		assertThat(vms.size(), is(1));
		assertThat(vms.get(0).getState(), is("state"));

		// Update state

		vmState = "newstate";
		vm = new Vm("instance1", firstCloud, vmState, username, connector.isVmUsable(vmState));
		vms = new ArrayList<Vm>();
		vms.add(vm);
		Collector.update(vms, username, firstCloud);

		vms = Vm.list(user);
		assertThat(vms.size(), is(1));
		assertThat(vms.get(0).getState(), is("newstate"));
	}

	@Test public void listByRun() {
		List<Vm> vmList = new ArrayList<Vm>();
		String vmState = "newstate";

		// First cloud
		Vm vm = new Vm("instance1", firstCloud, "state", username, connector.isVmUsable(vmState));
		vm.setRunUuid("runUuid1");
		vmList.add(vm);

		vm = new Vm("instance2", firstCloud, "state", username, connector.isVmUsable(vmState));
		vm.setRunUuid("runUuid1");
		vmList.add(vm);

		vm = new Vm("instance3", firstCloud, "state", username, connector.isVmUsable(vmState));
		vm.setRunUuid("runUuid2");
		vmList.add(vm);

		Collector.update(vmList, username, firstCloud);

		Map<String, List<Vm>> vmMap = Vm.listByRun("runUuid1");
		assertThat(vmMap.size(), is(1)); // 1 cloud in this run
		assertThat(vmMap.get(firstCloud).size(), is(2)); // 2 instances in this run

		vmMap = Vm.listByRun("runUuid2");
		assertThat(vmMap.size(), is(1)); // 1 cloud in this run
		assertThat(vmMap.get(firstCloud).size(), is(1)); // 1 instance in this run

		// Second cloud
		vmList = new ArrayList<Vm>();
		vm = new Vm("instance1", secondCloud, "state", username, connector.isVmUsable(vmState));
		vm.setRunUuid("runUuid1");
		vmList.add(vm);

		Collector.update(vmList, username, secondCloud);

		vmMap = Vm.listByRun("runUuid1");
		assertThat(vmMap.size(), is(2)); // 2 clouds in this run

		vmMap = Vm.listByRun("runUuid2");
		assertThat(vmMap.size(), is(1)); // 1 cloud in this run
	}

}
