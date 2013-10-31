package com.sixsq.slipstream.common.util;

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

import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.ModuleView;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.User;

public class CommonTestUtil {

	protected static final String PASSWORD = "password";

	// Need to set cloudServiceName before the status user is
	// created, since the createUser method uses it
	public static final String cloudServiceName = new LocalConnector()
			.getCloudServiceName();

	public static User createTestUser() {
		return createUser("test", PASSWORD);
	}

	public static User createUser(String name) {
		return CommonTestUtil.createUser(name, null);
	}

	public static User createUser(String name, String password) {
		User user = User.loadByName(name);
		if(user != null) {
			try {
				user.remove();
			} catch (Exception ex) {
				
			}
		}
		
		try {
			user = new User(name);
		} catch (ValidationException e) {
			e.printStackTrace();
			throw (new SlipStreamRuntimeException(e));
		}
		try {
			user.hashAndSetPassword(password);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			fail();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}

		try {
			user.setDefaultCloudServiceName(cloudServiceName);
		} catch (ValidationException e) {
			throw (new SlipStreamRuntimeException(e));
		}

		return user.store();
	}

	public static void deleteUser(User user) {
		if (user != null) {
			user.remove();
		}
	}

	public static DeploymentModule createDeployment()
			throws ValidationException, NotFoundException {

		ImageModule imageForDeployment1 = new ImageModule(
				"imagefordeployment1");
		imageForDeployment1
				.setParameter(new ModuleParameter("pi1", "pi1 init value",
						"pi1 parameter desc", ParameterCategory.Input));
		imageForDeployment1.setParameter(new ModuleParameter("po1",
				"po1 init value", "po1 parameter desc",
				ParameterCategory.Output));

		imageForDeployment1.setIsBase(true);
		imageForDeployment1.setImageId("123", CommonTestUtil.cloudServiceName);
		imageForDeployment1 = imageForDeployment1.store();

		ImageModule imageForDeployment2 = new ImageModule(
				"imagefordeployment2");
		imageForDeployment2
				.setParameter(new ModuleParameter("pi2", "pi2 init value",
						"pi2 parameter desc", ParameterCategory.Input));
		imageForDeployment2.setParameter(new ModuleParameter("po2",
				"po2 init value", "po2 parameter desc",
				ParameterCategory.Output));
		imageForDeployment2.setImageId("123", CommonTestUtil.cloudServiceName);
		imageForDeployment2 = imageForDeployment2.store();

		DeploymentModule deployment = new DeploymentModule("test/deployment");

		Node node;

		node = new Node("node1", imageForDeployment1);
		deployment.getNodes().put(node.getName(), node);

		node = new Node("node2", imageForDeployment2);
		deployment.getNodes().put(node.getName(), node);

		return deployment.store();
	}

	public static void deleteDeployment(DeploymentModule deployment) {
		for (Node n : deployment.getNodes().values()) {
			n.getImage().remove();
		}
		deployment.remove();
	}

	public static void resetAndLoadConnector(
			Class<? extends Connector> connectorClass)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {

		// Instantiate the configuration force loading from disk
		// This is required otherwise the first loading from
		// disk will reset the connectors
		Configuration.getInstance();

		Map<String, Connector> connectors = new HashMap<String, Connector>();
		Connector connector = ConnectorFactory
				.instantiateConnectorFromName(connectorClass.getName());
		connectors.put(connector.getCloudServiceName(), connector);
		ConnectorFactory.setConnectors(connectors);
	}

	public static void cleanupModules() {
		List<ModuleView> moduleViewList = Module
				.viewList(Module.RESOURCE_URI_PREFIX);
		for(ModuleView m : moduleViewList) {
			Module.loadByName(m.getName()).remove();
		}
	}

	// Only static methods. Ensure no instances are created.
	public CommonTestUtil() {

	}

	
}
