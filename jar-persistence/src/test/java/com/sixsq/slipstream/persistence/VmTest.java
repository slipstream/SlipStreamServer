package com.sixsq.slipstream.persistence;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class VmTest {

	@Test
	public void update() {
		
		// Insert one
		Vm vm = new Vm("instance1","cloud", "state", "user");
		List<Vm> vms = new ArrayList<Vm>();
		vms.add(vm);
		Vm.update(vms, "user");
		vms = Vm.list("user");
		
		assertThat(vms.size(), is(1));
		
		// Replace by another
		vm = new Vm("instance2","cloud", "state", "user");
		vms = new ArrayList<Vm>();
		vms.add(vm);
		Vm.update(vms, "user");

		vms = Vm.list("user");
		assertThat(vms.size(), is(1));
		assertThat(vms.get(0).getInstanceId(), is("instance2"));
	}
}
