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
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

public class DeploymentModuleTest {

	@Test
	public void validateTestDeploymentOk() throws ValidationException {

		DeploymentModule deployment = new DeploymentModule(
				"Deployments/deploymentOk");
		deployment.validate();
	}

	@Test
	public void validateTestDeploymentWithString() throws ValidationException {

		DeploymentModule deployment = new DeploymentModule(
				"Deployments/deploymentWithString");
		deployment.validate();
	}

	@Test
	public void validateApacheDeployment() throws ValidationException {
		DeploymentModule deployment = new DeploymentModule(
				"Examples/Apache/deployment");
		deployment.validate();
	}

	@Test
	public void verifyCorrectName() throws SlipStreamClientException {

		String name = "dummy";
		String resourceUrl = Module.RESOURCE_URI_PREFIX + name;

		DeploymentModule module = new DeploymentModule(name);

		assertEquals(name, module.getName());
		assertEquals(resourceUrl, module.getResourceUri());
		assertEquals(ModuleCategory.Deployment, module.getCategory());

	}

	@Test
	public void storeRetrieveAndDelete() throws SlipStreamClientException {

		String name = "dummy";
		String resourceUrl = Module.RESOURCE_URI_PREFIX + name;

		Module module = new DeploymentModule(name);
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

		String name = "moduleWithParameters";

		Module module = new DeploymentModule(name);

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
	public void moduleWithParameterMappings() throws SlipStreamClientException {

		String name = "moduleWithParameterMappings";

		DeploymentModule module = new DeploymentModule(name);

		String resourceUrl = module.getResourceUri();

		ImageModule image = new ImageModule("image1");
		image.store();

		Node node = new Node("node1", image);
		node = (Node) node.store();

		String parameterName = "name";
		String description = "description";
		String value = "node2:po1";

		NodeParameter np = new NodeParameter(parameterName, value, description);
		node.setParameterMapping(np);
		node = (Node) node.store();

		module.setNode(node);

		module.store();

		DeploymentModule moduleRestored = (DeploymentModule) Module
				.load(resourceUrl);
		assertNotNull(moduleRestored);

		Map<String, Node> nodes = moduleRestored.getNodes();

		assertThat(nodes.size(), is(1));

		node = nodes.get("node1");

		Map<String, NodeParameter> parameters = node.getParameterMappings();
		assertNotNull(parameters);
		assertThat(parameters.size(), is(1));

		np = parameters.get("name");
		assertNotNull(np);

		assertThat(np.getName(), is(parameterName));
		assertThat(np.getName(), is(parameterName));
		assertThat(np.getName(), is(parameterName));

		module.remove();
		assertNull(Module.load(resourceUrl));
		assertNotNull(ImageModule.load(image.getResourceUri()));
		image.remove();
		assertNull(ImageModule.load(image.getResourceUri()));
	}

	@Test(expected = ValidationException.class)
	public void moduleWithNonExistantNodeNameMapping()
			throws SlipStreamClientException {

		String name = "moduleWithNonExistantNodeNameMapping";

		DeploymentModule deployment = new DeploymentModule(name);

		ImageModule image = new ImageModule("image1");
		image.getParameters().put("pi1", new ModuleParameter("pi1", "pi1 init value", ""));
		image.store();

		Node node = new Node("node1", image);

		String parameterName = "pi1";
		String description = "description";
		String value = "node_doesnt_exist:po1";

		NodeParameter np = new NodeParameter(parameterName, value, description);
		node.setParameterMapping(np);

		deployment.setNode(node);

		try {
			deployment.validate();
		} catch (ValidationException ex) {
			assertThat(ex.getMessage(),
					containsString("node not defined in the deployment"));
			throw(ex);
		} finally {
			image.remove();
		}
		fail("Validation failed to find the problem");
	}

	@Test(expected = ValidationException.class)
	public void moduleWithNonExistantOutputParamaterNameMapping()
			throws SlipStreamClientException {

		String name = "moduleWithNonExistantOutputParamaterNameMapping";

		DeploymentModule deployment = new DeploymentModule(name);

		ModuleParameter parameter;
		
		ImageModule image = new ImageModule("image1");
		parameter = new ModuleParameter("pi1", "pi1 init value", "");
		parameter.setCategory(ParameterCategory.Input);
		image.getParameters().put(parameter.getName(), parameter);

		parameter = new ModuleParameter("po1", "po1 init value", "");
		parameter.setCategory(ParameterCategory.Output);
		image.getParameters().put(parameter.getName(), parameter);

		image.store();

		Node node = new Node("node1", image);

		String parameterName = "pi1";
		String value = "node2:po1";

		NodeParameter np = new NodeParameter(parameterName, value);
		node.setParameterMapping(np);

		node = new Node("node2", image);

		parameterName = "pi1";
		value = "node1:param_doesnt_exist";

		np = new NodeParameter(parameterName, value);
		node.setParameterMapping(np);

		deployment.setNode(node);

		try {
			deployment.validate();
		} catch (ValidationException ex) {
			assertThat(ex.getMessage(),
					containsString("node not defined in the deployment"));
			throw(ex);
		} finally {
			image.remove();
		}
		fail("Validation failed to find the problem");
	}

	@Test
	public void verifyModuleViewList() throws ValidationException {

		// clean-up
		ModuleTestUtil.cleanupModules();

		Module module1 = new DeploymentModule("module1");
		module1.store();

		Module module2 = new DeploymentModule("module2");
		module2.store();

		Module module3 = new DeploymentModule("module3");
		module3.store();

		List<ModuleView> moduleViewList = Module
				.viewList(Module.RESOURCE_URI_PREFIX);
		assertEquals(3, moduleViewList.size());

		Set<String> retrievedUsernames = new TreeSet<String>();
		for (ModuleView view : moduleViewList) {
			retrievedUsernames.add(view.getName());
		}

		Set<String> activeUsernames = new TreeSet<String>();
		activeUsernames.add("module1");
		activeUsernames.add("module2");
		activeUsernames.add("module3");

		assertEquals(activeUsernames, retrievedUsernames);

		module1.remove();
		module2.remove();
		module3.remove();
	}

	@Test
	public void checkModuleSerialization() throws ValidationException {

		Module module = new DeploymentModule("checkModuleSerialization");
		module.store();

		SerializationUtil.toXmlString(module);

		module.remove();
	}

}
