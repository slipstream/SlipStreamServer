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

import org.junit.Test;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;


public class AuthzTest {

	@Test
	public void emptyInitalGroup() throws ValidationException, ConfigurationException {
		Authz a = new Authz("user", new ImageModule("module"));
		assertThat(a.getGroupMembers().isEmpty(), is(true));
		a.setGroupMembers(new ArrayList<String>());
		assertThat(a.getGroupMembers().isEmpty(), is(true));
	}
	
	
	@Test
	public void addGroup() throws ValidationException, ConfigurationException {
		Authz a = new Authz("user", new ImageModule("module"));
		a.setInheritedGroupMembers(false);
		a.addGroupMember("my-friend");
		assertThat(a.getGroupMembers().size(), is(1));
		assertThat(a.getGroupMembers().contains("my-friend"), is(true));
	}

	@Test
	public void addGroupIsIdemPotent() throws ValidationException, ConfigurationException {
		Authz a = new Authz("user", new ImageModule("module"));
		a.setInheritedGroupMembers(false);
		a.addGroupMember("my-friend");
		assertThat(a.getGroupMembers().size(), is(1));
		a.addGroupMember("my-friend");
		assertThat(a.getGroupMembers().size(), is(1));
		assertThat(a.getGroupMembers().contains("my-friend"), is(true));
	}

	@Test
	public void setGroupMultiple() throws ValidationException, ConfigurationException {
		Authz a = new Authz("user", new ImageModule("module"));
		a.setInheritedGroupMembers(false);
		a.setGroupMembers("a,b, c,  ");
		assertThat(a.getGroupMembers().size(), is(3));
		assertThat(a.getGroupMembers().contains("a"), is(true));
		assertThat(a.getGroupMembers().contains("b"), is(true));
		assertThat(a.getGroupMembers().contains("c"), is(true));
	}


	@Test
	public void setGroupWithMultiplyDefined() throws ValidationException, ConfigurationException {
		Authz a = new Authz("user", new ImageModule("module"));
		a.setInheritedGroupMembers(false);
		a.setGroupMembers("a,b,c,c,c");
		assertThat(a.getGroupMembers().size(), is(3));
		assertThat(a.getGroupMembers().contains("a"), is(true));
		assertThat(a.getGroupMembers().contains("b"), is(true));
		assertThat(a.getGroupMembers().contains("c"), is(true));
	}
	
	@Test
	public void canGetViaGroup() throws ValidationException, ConfigurationException {
		User owner = new User("owner");
		User user = new User("user");
		Authz a = new Authz(owner.getName(), new ImageModule("module"));
		a.setInheritedGroupMembers(false);
		a.addGroupMember("user");
		assertThat(a.canGet(user), is(false));
		a.setGroupGet(true);
		assertThat(a.canGet(user), is(true));
	}
	
	@Test
	public void canPutViaGroup() throws ValidationException, ConfigurationException {
		User owner = new User("owner");
		User user = new User("user");
		Authz a = new Authz(owner.getName(), new ImageModule("module"));
		a.setInheritedGroupMembers(false);
		a.addGroupMember("user");
		assertThat(a.canPut(user), is(false));
		a.setGroupPut(true);
		assertThat(a.canPut(user), is(true));
	}
	
	@Test
	public void canPostViaGroup() throws ValidationException, ConfigurationException {
		User owner = new User("owner");
		User user = new User("user");
		Authz a = new Authz(owner.getName(), new ImageModule("module"));
		a.setInheritedGroupMembers(false);
		a.addGroupMember("user");
		assertThat(a.canPost(user), is(false));
		a.setGroupPost(true);
		assertThat(a.canPost(user), is(true));
	}
	
	@Test
	public void canDeleteViaGroup() throws ValidationException, ConfigurationException {
		User owner = new User("owner");
		User user = new User("user");
		Authz a = new Authz(owner.getName(), new ImageModule("module"));
		a.setInheritedGroupMembers(false);
		a.addGroupMember("user");
		assertThat(a.canDelete(user), is(false));
		a.setGroupDelete(true);
		assertThat(a.canDelete(user), is(true));
	}
	
	@Test
	public void canGetViaInheritedGroup() throws ValidationException {
		User owner = new User("owner");
		User user = new User("user");

		List<Metadata> parents = new ArrayList<Metadata>();
		
		Module parent = new ProjectModule("parent");
		Authz parentAuthz = new Authz(owner.getName(), parent);
		parentAuthz.setGroupGet(true);
		parentAuthz.addGroupMember("user");
		parentAuthz.setInheritedGroupMembers(false);
		parents.add(parent.store());
		
		Module module = new ImageModule("parent/module");
		Authz moduleAuthz = new Authz(owner.getName(), module);
		
		// can't get: no group access
		assertThat(moduleAuthz.canGet(user), is(false));

		// in the group members list, but not set in group
		moduleAuthz.setInheritedGroupMembers(true);
		assertThat(moduleAuthz.canGet(user), is(false));

		// now the group is set and driven by inherited group list
		moduleAuthz.setGroupGet(true);
		assertThat(moduleAuthz.canGet(user), is(true));

		// reset parent group members
		parentAuthz.setGroupMembers("");
		parents.add(parent.store());
		// The parent is cached in the child, so we clear it
		((Module)moduleAuthz.getGuarded()).clearGuardedParent();
		assertThat(moduleAuthz.canGet(user), is(false));

		for(Metadata p : parents) {
			p.remove();
		}
	}
	
	@Test
	public void cannotGetViaNestedInheritedGroupIfNotInParentGroup() throws ValidationException {
		User owner = new User("owner");
		User user = new User("user");

		Module project = new ProjectModule("parent/project");
		Authz projectAuthz = new Authz(owner.getName(), project);
		projectAuthz.setInheritedGroupMembers(true);
		project = project.store();
		
		Module parent = new ProjectModule("parent");
		Authz parentAuthz = new Authz(owner.getName(), parent);
		parentAuthz.addGroupMember("other");
		parentAuthz.setInheritedGroupMembers(false);
		parent.store();

		Module module = new ImageModule("parent/project/module");
		Authz moduleAuthz = new Authz(owner.getName(), module);
		moduleAuthz.setGroupGet(true);
		module.store();
		
		// group can view but user not in parent group
		assertThat(moduleAuthz.canGet(user), is(false));

		// open to parent group
		parent.getAuthz().addGroupMember("user");
		parent.store();
		
		module = Module.load(module.getResourceUri());

		// now user can view
		assertThat(module.getAuthz().canGet(user), is(true));

		module.remove();
		project.remove();
		parent.remove();
		
	}
	
	@Test
	public void canActionViaPublic() throws ValidationException {
		User owner = new User("owner");
		User user = new User("user");

		Module module = new ImageModule("module");
		Authz moduleAuthz = new Authz(owner.getName(), module);
		assertThat(moduleAuthz.canGet(user), is(false));
		moduleAuthz.setPublicGet(true);
		assertThat(moduleAuthz.canGet(user), is(true));
		
		assertThat(moduleAuthz.canPut(user), is(false));
		moduleAuthz.setPublicPut(true);
		assertThat(moduleAuthz.canPut(user), is(true));
		
		assertThat(moduleAuthz.canPost(user), is(false));
		moduleAuthz.setPublicPost(true);
		assertThat(moduleAuthz.canPost(user), is(true));
		
		assertThat(moduleAuthz.canDelete(user), is(false));
		moduleAuthz.setPublicDelete(true);
		assertThat(moduleAuthz.canDelete(user), is(true));
		
		module.remove();
	}
	
}
