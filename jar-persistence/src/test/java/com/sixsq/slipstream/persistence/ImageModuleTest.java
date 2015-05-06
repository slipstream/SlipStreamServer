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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.ModuleView;
import com.sixsq.slipstream.util.ModuleTestUtil;
import com.sixsq.slipstream.util.SerializationUtil;

public class ImageModuleTest {

	@Test(expected = ValidationException.class)
	public void testConstructorWithNullName() throws ValidationException {
		new ImageModule(null);
	}

	@Test(expected = ValidationException.class)
	public void testConstructorWithNull() throws ValidationException {
		new ImageModule(null);
	}

	@Test
	public void verifyCorrectName() throws SlipStreamClientException {

		String name = "dummy";
		String resourceUrl = Module.RESOURCE_URI_PREFIX + name;

		Module module = new ImageModule(name);

		assertEquals(name, module.getName());
		assertEquals(resourceUrl, module.getResourceUri());
		assertEquals(ModuleCategory.Image, module.getCategory());

	}

	@Test
	public void storeRetrieveAndDelete() throws SlipStreamClientException {

		String name = "dummy";
		String resourceUrl = Module.constructResourceUri(name);

		Module module = new ImageModule(name);
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
	public void moduleWithParameters() throws SlipStreamClientException {

		String name = "dummy2";

		Module module = new ImageModule(name);

		String resourceUrl = module.getResourceUri();

		String parameterName = "name";
		String description = "description";
		String value = "value";

		Parameter parameter = new ModuleParameter(parameterName, value,
				description);
		module.setParameter(parameter);

		module.store();

		Module moduleRestored = Module.load(resourceUrl);
		assertNotNull(moduleRestored);

		Map<String, Parameter> parameters = moduleRestored
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
		ModuleTestUtil.cleanupModules();

		Module module1 = new ImageModule("module1");
		module1.store();

		Module module2 = new ImageModule("module2");
		module2.store();

		Module module3 = new ImageModule("module3");
		module3.store();

		Set<String> activeUsernames = new TreeSet<String>();
		activeUsernames.add("module1");
		activeUsernames.add("module2");
		activeUsernames.add("module3");

		List<ModuleView> moduleViewList = Module
				.viewList(Module.RESOURCE_URI_PREFIX);
		assertEquals(3, moduleViewList.size());

		Set<String> retrievedUsernames = new TreeSet<String>();
		for (ModuleView view : moduleViewList) {
			retrievedUsernames.add(view.getName());
		}

		assertEquals(activeUsernames, retrievedUsernames);

		module1.remove();
		module2.remove();
		module3.remove();
	}

	@Test
	public void publishedModuleViewList() throws ValidationException {

		Module module1 = new ImageModule("module1");
		module1.publish();
		module1.store();

		Module module2 = new ImageModule("module2");
		module2.store();

		List<ModuleView> moduleViewList = Module
				.viewPublishedList();
		assertEquals(1, moduleViewList.size());

		module1.remove();
		module2.remove();
	}

	@Test
	public void unpublish() throws ValidationException {

		Module module = new ImageModule("moduleUnpublished");
		module.publish();
		module.store(false);
		module = Module.loadLatest(module.getResourceUri());
		assertNotNull(module.getPublished());

		module.unpublish();
		module.store(false);
		module = Module.loadLatest(module.getResourceUri());
		assertNull(module.getPublished());

		module.remove();
	}

	@Test
	public void publish() throws ValidationException {

		Module module = new ImageModule("moduleUnpublished");
		module.publish();
		module.store(false);

		module = Module.loadLatest(module.getResourceUri());

		assertNotNull(module.getPublished());

		module.remove();
	}

	@Test
	public void storeNoVersionIncrement() throws ValidationException {

		Module module = new ImageModule("storeNoVersionIncrement");
		module.store();

		module = Module.loadLatest(module.getResourceUri());
		int version = module.getVersion();

		module.store(false);

		module = Module.loadLatest(module.getResourceUri());

		assertThat(module.getVersion(), is(version));

		module.remove();
	}

	@Test
	public void checkXmlModuleSerialization() throws ValidationException {

		Module module = new ImageModule("checkXmlModuleSerialization");
		module.store();

		SerializationUtil.toXmlString(module);

		module.remove();
	}

	@Test
	public void checkJsonModuleSerialization() throws ValidationException {

		Module module;
		module = new ProjectModule("checkJsonModuleSerializationProject");
		SerializationUtil.toJsonString(module);
		module = new DeploymentModule("checkJsonModuleSerializationDeployment");
		SerializationUtil.toJsonString(module);
		module = new ImageModule("checkJsonModuleSerializationImage");
		SerializationUtil.toJsonString(module);
		
		module.remove();
	}

	@Test
	public void moduleInitializedWithDummyParameters()
			throws ValidationException {

		Module module = new ImageModule("moduleInitializedWithDummyParameters");

		assertNotNull(module.getParameters(ParameterCategory.Input.name()));
		assertNotNull(module.getParameters(ParameterCategory.Output.name()));

		assertThat(module.getParameters(ParameterCategory.Input.name()).size(),
				is(0));
		assertThat(
				module.getParameters(ParameterCategory.Output.name()).size(),
				is(greaterThan(0)));
	}

	@Test
	public void copyModule() throws ValidationException {
		ImageModule image = new ImageModule("image");
		ImageModule copy = image.copy();
		assertThat(image.getName(), is(copy.getName()));
		assertThat(image, is(not(copy)));
	}

	@Test
	public void getEmptyNotes() throws ValidationException {
		ImageModule image = new ImageModule("child");
		String[] notes = image.getNotes();
		assertThat(notes.length, is(0));
	}

	@Test
	public void getNotes() throws ValidationException {
		ImageModule image = new ImageModule("child");
		image.setNote("my note");
		String[] notes = image.getNotes();
		assertThat(notes.length, is(1));
		assertThat(notes[0], is("my note"));
	}

	@Test
	public void getInheritedNotes() throws ValidationException {
		ImageModule parent = new ImageModule("parent");
		parent.setNote("parent note");
		parent.store();
		ImageModule child = new ImageModule("child");
		child.setNote("child note");
		child.setModuleReference(parent);
		String[] notes = child.getNotes();
		assertThat(notes.length, is(2));
		assertThat(notes[0], is("parent note"));
		assertThat(notes[1], is("child note"));
		parent.remove();
	}

}