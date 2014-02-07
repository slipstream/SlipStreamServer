package com.sixsq.slipstream.persistence;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class VmTest {

	@Test
	public void update() {
		
		String keepMeCloud = "keepMeCloud";
		String cloud = "cloud";
		
		// Insert one
		
		Vm vm = new Vm("instance1",keepMeCloud, "state", "user");
		List<Vm> vms = new ArrayList<Vm>();
		vms.add(vm);
		Vm.update(vms, "user", cloud);
		vms = Vm.list("user");
		assertThat(vms.size(), is(1));
	
		vm = new Vm("instance2",cloud, "state", "user");
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
}
