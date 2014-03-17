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
import java.util.Iterator;
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
		List<Vm> vms = new ArrayList<Vm>();
		vms.add(new Vm("instance1", "cloud1", "running", "user1"));
		vms.add(new Vm("instance2", "cloud1", "running", "user1"));
		vms.add(new Vm("instance3", "cloud1", "terminated", "user1"));
		Vm.update(vms, "user1", "cloud1");

		vms.clear();
		vms.add(new Vm("instance4", "cloud2", "running", "user1"));
		Vm.update(vms, "user1", "cloud2");

		Map<String, Integer> usage = Vm.usage("user1");
		assertThat(usage.size(), is(2));

		assertThat(usage.get("cloud1"), is(2));
		assertThat(usage.get("cloud2"), is(1));

	}
}
