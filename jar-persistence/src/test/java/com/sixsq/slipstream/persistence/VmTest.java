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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VmTest {

	private static String firstCloud = "firstCloud";
	private static String secondCloud = "secondCloud";
	private static String user = "user";

	@Before
	public void setup() {
		for(Vm vm : Vm.list(user)) {
			vm.remove();
		}
		assertThat(Vm.list(user).size(), is(0));
	}

	@After
	public void tearDown() {
		Vm.update(new ArrayList<Vm>(), user, firstCloud);
		Vm.update(new ArrayList<Vm>(), user, secondCloud);
	}

	@Test
	public void update() {

		// Insert in one cloud

		Vm vm = new Vm("fistCloudInstance1", firstCloud, "state", user);
		List<Vm> firstCloudVms = new ArrayList<Vm>();
		firstCloudVms.add(vm);
		Vm.update(firstCloudVms, user, firstCloud);
		firstCloudVms = Vm.list(user);
		assertThat(firstCloudVms.size(), is(1));

		// Insert one in another cloud

		vm = new Vm("secondCloudInstance1", secondCloud, "state", user);
		List<Vm> secondCloudVms = new ArrayList<Vm>();
		secondCloudVms.add(vm);
		Vm.update(secondCloudVms, user, secondCloud);
		assertThat(Vm.list(user).size(), is(2));

		// Insert a second in the first cloud

		vm = new Vm("fistCloudInstance2", firstCloud, "state", user);
		firstCloudVms.add(vm);
		Vm.update(firstCloudVms, user, firstCloud);
		assertThat(Vm.list(user).size(), is(3));

		// Replace first cloud vms
		vm = new Vm("fistCloudInstance3", firstCloud, "state", user);
		firstCloudVms = new ArrayList<Vm>();
		firstCloudVms.add(vm);
		int removed = Vm.update(firstCloudVms, user, firstCloud);

		assertThat(removed, is(2));

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
		List<Vm> vms = new ArrayList<Vm>();

		// cloud1
		vm = new Vm("instance1", firstCloud, "running", "user1");
		vm.setRunUuid("e06cf0dd-5266-472d-90b4-2a1a27af9dfa");
		vms.add(vm);
		vm = new Vm("instance2", firstCloud, "Running", "user1");
		vm.setRunUuid("10ac7940-6151-4d0f-b90c-0213b094bcd0");
		vms.add(vm);
		vm = new Vm("instance3", firstCloud, "terminated", "user1");

		vm.setRunUuid("abf3a90f-c024-411b-8536-164b4617c636");
		vms.add(vm);
		vm = new Vm("instance5", firstCloud, "running", "user1");
		vm.setRunUuid("Unknown");
		vms.add(vm);

		Vm.update(vms, "user1", firstCloud);

		// cloud2
		vms.clear();
		vm = new Vm("instance4", secondCloud, "on", "user1");
		vm.setRunUuid("8e519c11-e46b-43d0-a370-c738655e1c06");
		vms.add(vm);

		Vm.update(vms, "user1", secondCloud);

		// actual tests
		Map<String, Integer> usage = Vm.usage("user1");
		assertThat(usage.size(), is(2));
		assertThat(usage.get(firstCloud), is(2));
		assertThat(usage.get(secondCloud), is(1));
	}

	@Test
	public void updateState() {

		// Insert in one cloud

		Vm vm = new Vm("instance1", firstCloud, "state", user);
		List<Vm> vms = new ArrayList<Vm>();
		vms.add(vm);
		Vm.update(vms, user, firstCloud);
		vms = Vm.list(user);
		assertThat(vms.size(), is(1));
		assertThat(vms.get(0).getState(), is("state"));

		// Update state

		vm = new Vm("instance1", firstCloud, "newstate", user);
		vms = new ArrayList<Vm>();
		vms.add(vm);
		int removed = Vm.update(vms, user, firstCloud);
		assertThat(removed, is(0));
		vms = Vm.list(user);
		assertThat(vms.size(), is(1));
		assertThat(vms.get(0).getState(), is("newstate"));
	}

	@Test
	public void empty() {
		List<Vm> vms = new ArrayList<Vm>();
		vms = Vm.list(user);
		assertThat(vms.size(), is(0));
	}

	@Test public void listByRun() {
		List<Vm> vmList = new ArrayList<Vm>();

		// First cloud
		Vm vm = new Vm("instance1", firstCloud, "state", user);
		vm.setRunUuid("runUuid1");
		vmList.add(vm);

		vm = new Vm("instance2", firstCloud, "state", user);
		vm.setRunUuid("runUuid1");
		vmList.add(vm);

		vm = new Vm("instance3", firstCloud, "state", user);
		vm.setRunUuid("runUuid2");
		vmList.add(vm);

		Vm.update(vmList, user, firstCloud);

		Map<String, List<Vm>> vmMap = Vm.listByRun("runUuid1");
		assertThat(vmMap.size(), is(1)); // 1 cloud in this run
		assertThat(vmMap.get(firstCloud).size(), is(2)); // 2 instances in this run

		vmMap = Vm.listByRun("runUuid2");
		assertThat(vmMap.size(), is(1)); // 1 cloud in this run
		assertThat(vmMap.get(firstCloud).size(), is(1)); // 1 instance in this run

		// Second cloud
		vmList = new ArrayList<Vm>();
		vm = new Vm("instance1", secondCloud, "state", user);
		vm.setRunUuid("runUuid1");
		vmList.add(vm);

		Vm.update(vmList, user, secondCloud);

		vmMap = Vm.listByRun("runUuid1");
		assertThat(vmMap.size(), is(2)); // 2 clouds in this run

		vmMap = Vm.listByRun("runUuid2");
		assertThat(vmMap.size(), is(1)); // 1 cloud in this run
	}

	@Test
	public void cloudInstanceIdUserMustBeUnique() throws Exception {
		boolean exceptionOccured = false;
		boolean firstInsertAccepted = false;
		try {
			EntityManager em = PersistenceUtil.createEntityManager();
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();

			String sqlInsert1 = String.format("INSERT INTO Vm VALUES (10, 'lokal', 'instance100', null, null, 'up', '%s', null, null, null)", user);
			String sqlInsert2 = String.format("INSERT INTO Vm VALUES (20, 'lokal', 'instance100', null, null, 'down', '%s', null, null, null)", user);

			Query query1 = em.createNativeQuery(sqlInsert1);
			Query query2 = em.createNativeQuery(sqlInsert2);

			assertEquals(1, query1.executeUpdate());
			firstInsertAccepted = true;

			query2.executeUpdate();
			transaction.commit();
		} catch (PersistenceException pe) {
			exceptionOccured = true;
		}

		assertTrue("First insert should have worked", firstInsertAccepted);
		assertTrue("Second insert should have failed", exceptionOccured);
	}

}
