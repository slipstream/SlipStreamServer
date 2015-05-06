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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.run.RuntimeParameterMediator;

public class VmRuntimeParameterMappingTest {

	private static User user;
	private String instanceIdParameterName = RuntimeParameter.constructParamName(Run.MACHINE_NAME,
			RuntimeParameter.INSTANCE_ID_KEY);
	private String vmstateParameterName = RuntimeParameter.constructParamName(Run.MACHINE_NAME,
			RuntimeParameter.STATE_VM_KEY);
	private String hostnameParameterName = RuntimeParameter.constructParamName(Run.MACHINE_NAME,
			RuntimeParameter.HOSTNAME_KEY);

	@BeforeClass
	public static void setupClass() throws ValidationException {
		user = new User("user");
		load();
	}

	@Before
	public void setup() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void getRuntimeParameter() throws ValidationException {

		Set<ConnectorInstance> clouds = new HashSet<ConnectorInstance>();
		clouds.add(new ConnectorInstance("cloud1", null));
		Module module = new ImageModule("module");
		Run run = new Run(module, RunType.Machine, clouds, user);
		run.store();
		RuntimeParameter vmstate;
		RuntimeParameter hostname;
		VmRuntimeParameterMapping mapping;

		vmstate = new RuntimeParameter(run, "machine:key11", "value11", "description");
		vmstate.store();
		hostname = new RuntimeParameter(run, "machine:key111", "value111", "description");
		hostname.store();
		mapping = new VmRuntimeParameterMapping("instanceId1", "cloud1", user.getName(), "machine", null, null, vmstate, hostname);
		mapping.store();

		vmstate = new RuntimeParameter(run, "machine:key21", "value21", "description");
		vmstate.store();
		hostname = new RuntimeParameter(run, "machine:key121", "value121", "description");
		hostname.store();
		mapping = new VmRuntimeParameterMapping("instanceId2", "cloud1", user.getName(), "machine", null, null, vmstate, hostname);
		mapping.store();

		vmstate = new RuntimeParameter(run, "machine:key31", "value31", "description");
		vmstate.store();
		hostname = new RuntimeParameter(run, "machine:key131", "value131", "description");
		hostname.store();
		mapping = new VmRuntimeParameterMapping("instanceId3", "cloud1", user.getName(), "machine", null, null, vmstate, hostname);
		mapping.store();

		vmstate = new RuntimeParameter(run, "machine:key12", "value12", "description");
		vmstate.store();
		hostname = new RuntimeParameter(run, "machine:key112", "value112", "description");
		hostname.store();
		mapping = new VmRuntimeParameterMapping("instanceId1", "cloud2", user.getName(), "machine", null, null, vmstate, hostname);
		mapping.store();

		mapping = VmRuntimeParameterMapping.find("cloud1", "instanceId2");
		assertThat(mapping.getVmstateRuntimeParameter().getName(), is("key21"));
		assertThat(mapping.getHostnameRuntimeParameter().getName(), is("key121"));

		mapping = VmRuntimeParameterMapping.find("cloud2", "instanceId1");
		assertThat(mapping.getVmstateRuntimeParameter().getName(), is("key12"));
		assertThat(mapping.getHostnameRuntimeParameter().getName(), is("key112"));

		for (VmRuntimeParameterMapping m : VmRuntimeParameterMapping.getMappings()) {
			m.remove();
		}
		run.remove();
	}

	@Test
	public void runVmVmMapping() throws ValidationException {

		Module image = new ImageModule();

		String cloudName = "local";

		// Configure run with cloud name
		String instanceId = "1234";

		String vmstate = "new state";

		Run run = createAndStoreRun(image, instanceId, cloudName);

		// set the runtime parameter instance in the vm mapping
		RuntimeParameter instanceIdRuntimeParameter = new RuntimeParameter(run, instanceIdParameterName, instanceId, "");
		RuntimeParameterMediator.processSpecialValue(instanceIdRuntimeParameter);

		VmRuntimeParameterMapping mapping = VmRuntimeParameterMapping.find("local", instanceId);

		assertThat(mapping.getRunUuid(), is(run.getUuid()));
		assertThat(mapping.getInstanceId(), is(instanceId));

		// Updating the vm state sets the corresponding runtime parameter
		Vm vm = new Vm(instanceId, cloudName, vmstate, user.getName());
		Vm.update(Arrays.asList(vm), user.getName(), cloudName);
		mapping = VmRuntimeParameterMapping.find("local", instanceId);
		assertThat(mapping.getRunUuid(), is(run.getUuid()));
		assertThat(vm.getRunUuid(), is(run.getUuid()));
		assertThat(mapping.getVmstateRuntimeParameter().getValue(), is(vmstate));
		assertThat(
				RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
						RuntimeParameter.constructParamName(Run.MACHINE_NAME, RuntimeParameter.INSTANCE_ID_KEY))
						.getValue(), is(instanceId));
		assertThat(RuntimeParameter.loadFromUuidAndKey(run.getUuid(), instanceIdParameterName).getValue(),
				is(instanceId));

		mapping.remove();
		run.remove();
	}

	private Run createAndStoreRun(Module image, String instanceId, String cloudName) throws ValidationException {
		Set<ConnectorInstance> clouds = new HashSet<ConnectorInstance>();
		clouds.add(new ConnectorInstance("cloud1", null));
		Run run = new Run(image, RunType.Run, clouds, user);

		String cloudRuntimeParameterName = RuntimeParameter.constructParamName(Run.MACHINE_NAME,
				RuntimeParameter.CLOUD_SERVICE_NAME);
		RuntimeParameter cloudRuntimeParameter = new RuntimeParameter(run, cloudRuntimeParameterName, cloudName, "");
		run.getRuntimeParameters().put(cloudRuntimeParameterName, cloudRuntimeParameter);

		RuntimeParameter instanceIdRuntimeParameter = new RuntimeParameter(run, instanceIdParameterName, instanceId, "");
		run.getRuntimeParameters().put(instanceIdParameterName, instanceIdRuntimeParameter);

		RuntimeParameter vmstateRuntimeParameter = new RuntimeParameter(run, vmstateParameterName, "", "");
		run.getRuntimeParameters().put(vmstateParameterName, vmstateRuntimeParameter);

		RuntimeParameter hostnameRuntimeParameter = new RuntimeParameter(run, hostnameParameterName, "", "");
		run.getRuntimeParameters().put(hostnameParameterName, hostnameRuntimeParameter);

		run.store();
		return run;
	}

	private static void load() {

		int MAX_VMS = 1000;

		Random random = new Random();
		String[] states = { "init", "run", "done", "error" };
		random.nextInt(states.length);
		List<Vm> vms = new ArrayList<Vm>();
		for (int i = 0; i < MAX_VMS; i++) {
			Vm vm = new Vm("instance_" + i, "local", states[random.nextInt(states.length)], user.getName());
			vms.add(vm);
		}
		Vm.update(vms, user.getName(), "local");

	}

}
