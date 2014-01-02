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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.sixsq.slipstream.common.util.CommonTestUtil;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.ModuleView;
import com.sixsq.slipstream.util.SerializationUtil;

public class ProjectModuleTest {

	@Test
	public void verifyCorrectName() throws SlipStreamClientException {

		String name = "dummy";
		String resourceUrl = Module.RESOURCE_URI_PREFIX + name;

		Module module = new ProjectModule(name);

		assertEquals(name, module.getName());
		assertEquals(resourceUrl, module.getResourceUri());
		assertEquals(ModuleCategory.Project, module.getCategory());

	}

	@Test
	public void storeRetrieveAndDelete() throws SlipStreamClientException {

		String name = "dummy";
		String resourceUrl = Module.RESOURCE_URI_PREFIX + name;

		Module module = new ProjectModule(name);
		module.store();

		Module moduleRestored = Module.load(resourceUrl);
		assertNotNull(moduleRestored);

		assertEquals(module.getName(), moduleRestored.getName());
		assertEquals(module.getResourceUri(), moduleRestored.getResourceUri());
		assertEquals(module.getCategory(), moduleRestored.getCategory());

		module.remove();
		moduleRestored = Module.load(resourceUrl);
		assertNull(moduleRestored);
	}

	@Test
	public void ModuleWithParameters() throws SlipStreamClientException {

		String name = "dummy2";

		Module module = new ProjectModule(name);

		String resourceUrl = module.getResourceUri();

		String parameterName = "name";
		String description = "description";
		String value = "value";

		ModuleParameter parameter = new ModuleParameter(parameterName, value,
				description);
		module.setParameter(parameter);

		module.store();

		Module moduleRestored = Module.load(resourceUrl);
		assertNotNull(moduleRestored);

		Map<String, ModuleParameter> parameters = moduleRestored
				.getParameters();
		assertNotNull(parameters);
		assertTrue(parameters.size() > 0);

		parameter = parameters.get(parameterName);
		assertNotNull(parameter);
		assertEquals(parameterName, parameter.getName());
		assertEquals(description, parameter.getDescription());
		assertEquals(value, parameter.getValue());

		module.remove();
		moduleRestored = Module.load(resourceUrl);
		assertNull(moduleRestored);
	}

	@Test
	public void verifyModuleViewList() throws ValidationException {

		// clean-up
		CommonTestUtil.cleanupModules();
		
		Module module1 = new ProjectModule("module1");
		module1.store();

		Module module2 = new ProjectModule("module2");
		module2.store();

		Module module3 = new ProjectModule("module3");
		module3.store();

		Set<String> modules = new TreeSet<String>();
		modules.add("module1");
		modules.add("module2");
		modules.add("module3");

		List<ModuleView> moduleViewList = Module
				.viewList(Module.RESOURCE_URI_PREFIX);
		assertEquals(3, moduleViewList.size());

		Set<String> retrievedUsernames = new TreeSet<String>();
		for (ModuleView view : moduleViewList) {
			retrievedUsernames.add(view.getName());
		}

		assertEquals(modules, retrievedUsernames);

		module1.remove();
		module2.remove();
		module3.remove();
	}

	@Test
	public void verifyProjectWithChildrenModuleViewList()
			throws ValidationException {

		ProjectModule parent = new ProjectModule("p");
		parent.store();

		Module module1 = new ProjectModule("p/module1");
		module1.store();

		Module module2 = new ProjectModule("p/module2");
		module2.store();

		Module module3 = new ProjectModule("p/module3");
		module3.store();

		Set<String> modules = new TreeSet<String>();
		modules.add("module1");
		modules.add("module2");
		modules.add("module3");

		parent = ProjectModule.load(parent.getResourceUri());

		assertThat(parent.getChildren().size(), is(3));

		Set<String> retrievedModules = new TreeSet<String>();
		for (ModuleView view : parent.getChildren()) {
			retrievedModules.add(view.getName());
		}

		assertEquals(modules, retrievedModules);

		module1.remove();
		module2.remove();
		module3.remove();
		parent.remove();
	}

	@Test
	public void checkModuleSerialization() throws ValidationException {

		Module module = new ProjectModule("module1");
		module.store();

		SerializationUtil.toXmlString(module);

		module.remove();
	}

	@Test
	public void loadingDoesntExistsReturnsNull() throws ValidationException {

		assertNull(ProjectModule.load("doesntexists"));
	}

}
