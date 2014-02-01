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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.NodeParameter;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.user.UserTest;
import com.sixsq.slipstream.util.ResourceTestBase;

public class ModuleResourceCopyToTest extends ResourceTestBase {

	private static User anotherUser;
	private ImageModule image;
	private ImageModule privateImage;
	private DeploymentModule deployment;
	private static ProjectModule publicProject;
	private static ProjectModule privateProjectAnother;
	private static final String NODE_NAME = "n1";
	private static final String PARAMETER_NAME = "parameter_name";

	@BeforeClass
	public static void setupClass() throws ValidationException,
			ConfigurationException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {

		resetAndLoadConnector(com.sixsq.slipstream.connector.local.LocalConnector.class);

		anotherUser = new User("anotherUser");
		anotherUser.setDefaultCloudServiceName(LocalConnector.CLOUD_SERVICE_NAME);
		UserTest.storeUser(anotherUser);
		
		user.setDefaultCloudServiceName(LocalConnector.CLOUD_SERVICE_NAME);
		user = (User) user.store();
		
		publicProject = new ProjectModule("ModuleResourceCopyToTestPublicProject");
		publicProject.getAuthz().setPublicCreateChildren(true);
		publicProject.getAuthz().setUser(user.getName());
		publicProject = publicProject.store();
		
		privateProjectAnother = new ProjectModule("ModuleResourceCopyToTestPrivateProject");
		privateProjectAnother.getAuthz().setGroupCreateChildren(false);
		privateProjectAnother.getAuthz().setUser(anotherUser.getName());
		privateProjectAnother = privateProjectAnother.store();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try {
			user.remove();			
		} catch(Exception ex) {
			// ok
		}
		try {
			anotherUser.remove();
		} catch(Exception ex) {
			// ok
		}
		try {
			publicProject.remove();
		} catch(Exception ex) {
			// ok
		}
		try {
			privateProjectAnother.remove();
		} catch(Exception ex) {
			// ok
		}
	}

	@Before
	public void setup() throws ValidationException {
		createModules("copy_to");
	}
	
	@After
	public void tearDown() {
		image.remove();
		deployment.remove();
		privateImage.remove();
	}
	
	@Test
	public void copySourceFormParameterMissing() throws ConfigurationException,
			SlipStreamClientException {

		Form form = new Form();
		Request request = createPostRequest(publicProject.getName(), form.getWebRepresentation(), user);
		addUserToRequest(user.getName(), request);

		Response response = executeRequest(request);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
	}

	@Test
	public void copyTargetNameFormParameterMissing() throws ConfigurationException,
			SlipStreamClientException {

		Form form = new Form();
		form.add(ModuleResource.COPY_SOURCE_FORM_PARAMETER_NAME, "something");
		Request request = createPostRequest(publicProject.getName(), form.getWebRepresentation(), user);
		addUserToRequest(user.getName(), request);

		Response response = executeRequest(request);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
	}

	@Test
	public void copySourceDoesntExist() throws ConfigurationException,
			SlipStreamClientException {

		Form form = new Form();
		form.add(ModuleResource.COPY_SOURCE_FORM_PARAMETER_NAME, "DoesntExist");

		Request request = createPostRequest(image.getName(), form.getWebRepresentation(), user);
		addUserToRequest(user.getName(), request);

		Response response = executeRequest(request);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
	}

	@Test
	public void copyTargetExists() throws ConfigurationException,
			SlipStreamClientException {

		Form form = new Form();
		form.add(ModuleResource.COPY_SOURCE_FORM_PARAMETER_NAME, image.getResourceUri());

		Request request = createPostRequest(image.getName(), form.getWebRepresentation(), user);
		addUserToRequest(user.getName(), request);

		Response response = executeRequest(request);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
	}
		
	@Test
	public void allGood() throws ConfigurationException,
			SlipStreamClientException {

		String targetName = "allGood";

		Form form = new Form();
		form.add(ModuleResource.COPY_SOURCE_FORM_PARAMETER_NAME, image.getResourceUri());
		form.add(ModuleResource.COPY_TARGET_FORM_PARAMETER_NAME, targetName);

		Request request = createPostRequest(publicProject.getName(), form.getWebRepresentation(), user);
		addUserToRequest(user.getName(), request);

		Response response = executeRequest(request);
		
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		Module.load(Module.constructResourceUri(publicProject.getName() + "/" + targetName)).remove();
	}
		
	@Test
	public void noReadRightOnSource() throws ConfigurationException,
			SlipStreamClientException {

		String targetName = "noReadRightOnSource";

		Form form = new Form();
		form.add(ModuleResource.COPY_SOURCE_FORM_PARAMETER_NAME, privateImage.getResourceUri());
		form.add(ModuleResource.COPY_TARGET_FORM_PARAMETER_NAME, targetName);

		Request request = createPostRequest(privateProjectAnother.getName(), form.getWebRepresentation(), user);
		addUserToRequest(anotherUser.getName(), request);

		Response response = executeRequest(request);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));
	}
		
	@Test
	public void noReadRightOnTarget() throws ConfigurationException,
			SlipStreamClientException {

		String targetName = "noReadRightOnTarget";

		Form form = new Form();
		form.add(ModuleResource.COPY_SOURCE_FORM_PARAMETER_NAME, image.getResourceUri());
		form.add(ModuleResource.COPY_TARGET_FORM_PARAMETER_NAME, targetName);

		Request request = createPostRequest(privateProjectAnother.getName(), form.getWebRepresentation(), user);
		addUserToRequest(user.getName(), request);

		Response response = executeRequest(request);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));
	}
		
	private void createModules(String moduleName) throws ValidationException {

		image = new ImageModule(moduleName);

		image.setImageId("123", cloudServiceName);

		image.setParameter(new ModuleParameter(PARAMETER_NAME, "default value",
				""));

		image.getCloudImageIdentifiers().add(
				new CloudImageIdentifier(image,
						LocalConnector.CLOUD_SERVICE_NAME, "abc"));

		image.getAuthz().setPublicGet(true);
		image.getAuthz().setUser(user.getName());

		image.store();

		privateImage = new ImageModule("private" + moduleName);

		privateImage.setImageId("123", cloudServiceName);

		privateImage.setParameter(new ModuleParameter(PARAMETER_NAME, "default value",
				""));

		privateImage.getCloudImageIdentifiers().add(
				new CloudImageIdentifier(privateImage,
						LocalConnector.CLOUD_SERVICE_NAME, "abc"));

		privateImage.getAuthz().setGroupGet(false);
		privateImage.getAuthz().setUser(user.getName());
		
		privateImage.store();

		Node node = new Node(NODE_NAME, image);
		node.setMultiplicity(1);
		node.setParameter(new NodeParameter(PARAMETER_NAME, "'default value'"));

		deployment = new DeploymentModule(moduleName + "Deployment");
		deployment.getNodes().put(node.getName(), node);
		deployment.store();
	}


	private Request createPostRequest(String name, Representation entity,
			User user) throws ConfigurationException {
		Request request = createPostRequest(createModuleAttributes(name), entity);
		addUserToRequest(user.getName(), request);
		return request;
	}

	protected Response executeRequest(Request request) {

		ServerResource resource = new ModuleResource();
		Response response = new Response(request);

		resource.init(null, request, response);
		if (response.getStatus().isSuccess()) {
			resource.handle();
		}

		return resource.getResponse();
	}

	protected Map<String, Object> createRequestAttributes(String module) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("module", module);
		return attributes;
	}

}
