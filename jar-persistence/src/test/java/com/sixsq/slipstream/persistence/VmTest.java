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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class VmTest {

	@Test
	public void update() {

		String keepMeCloud = "keepMeCloud";
		String cloud = "cloud";

		// Insert one

		Vm vm = new Vm("instance1", keepMeCloud, "state", "user");
		List<Vm> vms = new ArrayList<Vm>();
		vms.add(vm);
		Vm.update(vms, "user", cloud);
		vms = Vm.list("user");
		assertThat(vms.size(), is(1));

		vm = new Vm("instance2", cloud, "state", "user");
		vms = new ArrayList<Vm>();
		vms.add(vm);
		Vm.update(vms, "user", cloud);
		vms = Vm.list("user");
		assertThat(vms.size(), is(2));

		// Replace by another
		vm = new Vm("instance3",cloud, "state", "user");
		vms = new ArrayList<Vm>();
		vms.add(vm);
		Vm.update(vms, "user", cloud);

		vms = Vm.list("user");
		assertThat(vms.size(), is(2));
		assertThat(vms.get(0).getInstanceId(), is("instance1"));
		assertThat(vms.get(1).getInstanceId(), is("instance3"));
	}

	@Test
	public void usage() {
		Vm vm;
                List<Vm> vms = new ArrayList<Vm>();

		// cloud1
		vm = new Vm("instance1", "cloud1", "running", "user1");
		vm.setRunUuid("e06cf0dd-5266-472d-90b4-2a1a27af9dfa");
		vms.add(vm);
		vm = new Vm("instance2", "cloud1", "Running", "user1");
		vm.setRunUuid("10ac7940-6151-4d0f-b90c-0213b094bcd0");
		vms.add(vm);
		vm = new Vm("instance3", "cloud1", "terminated", "user1");

                vm.setRunUuid("abf3a90f-c024-411b-8536-164b4617c636");
		vms.add(vm);
		vm = new Vm("instance5", "cloud1", "running", "user1");
		vm.setRunUuid("Unknown");
		vms.add(vm);

		Vm.update(vms, "user1", "cloud1");

		// cloud2
		vms.clear();
		vm = new Vm("instance4", "cloud2", "on", "user1");
		vm.setRunUuid("8e519c11-e46b-43d0-a370-c738655e1c06");
		vms.add(vm);

		Vm.update(vms, "user1", "cloud2");

		// actual tests
		Map<String, Integer> usage = Vm.usage("user1");
		assertThat(usage.size(), is(2));
		assertThat(usage.get("cloud1"), is(2));
		assertThat(usage.get("cloud2"), is(1));
	}
}
