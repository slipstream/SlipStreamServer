package com.sixsq.slipstream.module;

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
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Form;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserTest;

public class DeploymentFormProcesorTest {

	private static final String PASSWORD = "password";
	private static User user = UserTest.createUser("test", PASSWORD);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		UserTest.storeUser(user);
	}

	@Test
	public void processDeploymentMapping() throws ConfigurationException,
			SlipStreamClientException {

		User user = UserTest.createUser("test");

		String imageName = "processDeploymentMappingImage";
		Module image = new ImageModule(imageName);
		image.setParameter(new ModuleParameter("pi1", "", "",
				ParameterCategory.Input));
		image.setParameter(new ModuleParameter("po1", "", "",
				ParameterCategory.Output));
		image.store();

		Form form = new Form();

		form.add("name", "node1");
		form.add("node--1--shortname", "node1");
		form.add("node--1--imagelink", "module/" + imageName);
		form.add("node--1--mappingtable--1--input", "pi1");
		form.add("node--1--mappingtable--1--output", "node2:po1");

		form.add("name", "node2");
		form.add("node--2--shortname", "node2");
		form.add("node--2--imagelink", "module/" + imageName);
		form.add("node--2--mappingtable--1--input", "pi1");
		form.add("node--2--mappingtable--1--output", "node1:po1");

		DeploymentFormProcessor processor = new DeploymentFormProcessor(user);

		processor.processForm(form);

		DeploymentModule module = (DeploymentModule) processor
				.getParametrized();

		assertThat(module.getNodes().size(), is(2));

		Node node = module.getNodes().get("node1");
		assertThat(node.getName(), is("node1"));
		assertThat(node.getImage().getName(), is(imageName));

		NodeParameter param = node.getParameterMappings().get("pi1");
		assertThat(param.getValue(), is("node2:po1"));

		image.remove();
	}

	@Test(expected = ValidationException.class)
	public void deploymentWithIllegalSelfReferencingNode()
			throws ConfigurationException, SlipStreamClientException {

		User user = UserTest.createUser("test");

		String imageName = "deploymentWithIllegalSelfReferencingNode";
		Module image = new ImageModule(imageName);
		image.setParameter(new ModuleParameter("pi1", "", "",
				ParameterCategory.Input));
		image.setParameter(new ModuleParameter("po1", "", "",
				ParameterCategory.Output));
		image.store();

		Form form = new Form();

		form.add("name", "node1");
		form.add("node--1--shortname", "node1");
		form.add("node--1--imagelink", "module/" + imageName);
		form.add("node--1--mappingtable--1--input", "pi1");
		form.add("node--1--mappingtable--1--output", "node1:po1");

		DeploymentFormProcessor processor = new DeploymentFormProcessor(user);

		try {
			processor.processForm(form);
		} finally {
			image.remove();
		}
	}

	@Test(expected = ValidationException.class)
	public void deploymentWithMissingMappingAndNoDefaultValue()
			throws ConfigurationException, SlipStreamClientException {

		User user = UserTest.createUser("test");

		String imageName = "deploymentWithMissingMappingAndNoDefaultValue";
		Module image = new ImageModule(imageName);
		image.setParameter(new ModuleParameter("pi1", null, "",
				ParameterCategory.Input));
		image.store();

		Form form = new Form();

		form.add("name", "node1");
		form.add("node--1--shortname", "node1");
		form.add("node--1--imagelink", "module/" + imageName);

		DeploymentFormProcessor processor = new DeploymentFormProcessor(user);

		try {
			processor.processForm(form);
		} finally {
			image.remove();
		}
	}

	@Test(expected = ValidationException.class)
	public void deploymentWithMissingMappingAndDefaultValue()
			throws ConfigurationException, SlipStreamClientException {

		User user = UserTest.createUser("test");

		String imageName = "deploymentWithMissingMappingAndDefaultValue";
		Module image = new ImageModule(imageName);
		image.setParameter(new ModuleParameter("pi1", "some_default", "",
				ParameterCategory.Input));
		image.store();

		Form form = new Form();

		form.add("name", "node1");
		form.add("node--1--shortname", "node1");
		form.add("node--1--imagelink", "module/" + imageName);

		DeploymentFormProcessor processor = new DeploymentFormProcessor(user);

		try {
			processor.processForm(form);
		} finally {
			image.remove();
		}
	}

}
