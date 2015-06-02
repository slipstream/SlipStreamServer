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
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.user.UserTest;
import com.sixsq.slipstream.util.ResourceTestBase;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.XmlUtil;

public class ModuleResourceTest extends ResourceTestBase {

	private static User anotherUser;

	@BeforeClass
	public static void setupClass() throws ValidationException,
			ConfigurationException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {

		resetAndLoadConnector(com.sixsq.slipstream.connector.local.LocalConnector.class);

		user = UserTest.createUser("userModuleResourceTest", "password");
		user.setDefaultCloudServiceName(cloudServiceName);
		user = UserTest.storeUser(user);

		anotherUser = new User("anotherUser");
		anotherUser.setDefaultCloudServiceName(cloudServiceName);
		anotherUser = UserTest.storeUser(anotherUser);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try {
			user.remove();
		} catch (Exception ex) {
			// ok
		}
		try {
			anotherUser.remove();
		} catch (Exception ex) {
			// ok
		}
	}

	@Test
	public void getModuleProject() throws ConfigurationException,
			SlipStreamClientException {

		String projectName = "getModuleProject";
		createAndStoreProject(projectName);

		Request request = createGetRequest(projectName, user);

		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_OK, response.getStatus());

		String externalFormatXml = response.getEntityAsText();
		String internalFormatXml = XmlUtil.denormalize(externalFormatXml);
		Module project = (Module) SerializationUtil.fromXml(internalFormatXml,
				ProjectModule.class);

		assertEquals(projectName, project.getName());

		project.remove();
	}

	@Test
	public void putModuleProjectAsForm() throws ConfigurationException,
			SlipStreamClientException, IOException {

		String projectName = "putModuleProjectAsForm";
		String description = "a description";
		Response response = putModuleAsForm(projectName, description);

		Module persistedProject = null;

		try {
			assertEquals(Status.SUCCESS_CREATED, response.getStatus());
			persistedProject = (Module) ProjectModule.load(Module
					.constructResourceUri(projectName));
			assertEquals(projectName, persistedProject.getName());
			assertEquals(description, persistedProject.getDescription());
		} finally {
			try {
				persistedProject.remove();
			} catch (NullPointerException ex) {

			}
		}
	}

	private Response putModuleAsForm(String projectName, String description)
			throws ConfigurationException, ValidationException {
		ModuleCategory category = ModuleCategory.Project;
		Form form = createForm(projectName, category);

		form.add("description", description);

		Request request = createPutRequest(projectName,
				form.getWebRepresentation(), user);

		return executeRequest(request);
	}

	@Test
	public void putExistingModuleWithCircularReference() throws ValidationException {
		String moduleName = "examples/imageCircularRef";

		Module image = new ImageModule(moduleName);
		image.setAuthz(new Authz(user.getName(), image));
		image.store();

		ModuleCategory category = ModuleCategory.Image;
		Form form = createForm(moduleName, category);
		form.add("moduleReference", moduleName);
		Request request = createPutRequest(moduleName, form.getWebRepresentation(), user);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));

		image.remove();
	}

	@Test
	public void putExistingModuleWithoutPutRightIsForbiden()
			throws ValidationException {
		String projectName = "existingProject";
		Module project = createAndStoreProject(projectName);
		ModuleCategory category = ModuleCategory.Project;
		Form form = createForm(projectName, category);
		Request request = createPutRequest(projectName,
				form.getWebRepresentation(), anotherUser);
		Response response = executeRequest(request);
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));
		project.remove();
	}

	@Test
	public void putNewModuleOnExistingModule() throws ValidationException {
		String projectName = "existingProject";
		Module project = createAndStoreProject(projectName);
		ModuleCategory category = ModuleCategory.Project;
		Form form = createForm(projectName, category);
		Request request = createPutRequest("existingProject",
				form.getWebRepresentation(), user);
		Response response = executeRequest(request);
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
		project.remove();
	}

	@Test
	public void putNewModuleOnExistingModuleIsForbiden()
			throws ValidationException {
		String projectName = "existingProject";
		Module project = createAndStoreProject(projectName);
		ModuleCategory category = ModuleCategory.Project;
		Form form = createForm(projectName, category);
		Request request = createPutRequest("existingProject",
				form.getWebRepresentation(), user, TEST_REQUEST_NAME
						+ "?new=true");
		Response response = executeRequest(request);
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));
		project.remove();
	}

	@Test
	public void putSubProjectProject() throws ConfigurationException,
			SlipStreamClientException, IOException {

		Module parentProject = createAndStoreProject("putSubProjectProject-Parent");

		String projectName = "p/putSubProjectProject";
		Module project = new ProjectModule(projectName);

		ModuleCategory category = ModuleCategory.Project;
		Form form = createForm(projectName, category);

		Request request = createPutRequest(projectName,
				form.getWebRepresentation(), user);

		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_CREATED, response.getStatus());

		project = ProjectModule.load(project.getId());

		project.remove();
		parentProject.remove();
	}

	private Form createForm(String projectName, ModuleCategory category) {
		Form form = new Form();
		form.add("name", projectName);
		form.add("category", category.toString());
		return form;
	}

	@Test
	@Ignore
	public void putModuleProjectAsXml() throws ConfigurationException,
			SlipStreamClientException {

		String projectName = "putModuleProjectAsXml";

		Module project = new ProjectModule(projectName);
		String content = SerializationUtil.toXmlString(project);

		StringRepresentation stringRepresentation = new StringRepresentation(
				content, MediaType.APPLICATION_XML);
		Request request = createPutRequest(projectName, stringRepresentation,
				user);

		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_OK, response.getStatus());
		Module persistedProject = (Module) ProjectModule.load("module/"
				+ projectName);
		assertEquals(projectName, persistedProject.getName());

		persistedProject.remove();
	}

	@Test
	public void getNewProjectToRetrieveTemplate()
			throws ConfigurationException, ValidationException {

		Map<String, Object> attributes = createAttributes("category",
				ModuleCategory.Project.toString());
		Request request = createGetRequest("new", user, attributes);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}

	@Test
	public void getNewMachineImageToRetrieveTemplate()
			throws ConfigurationException, ValidationException {

		Map<String, Object> attributes = createAttributes("category",
				ModuleCategory.Image.toString());
		Request request = createGetRequest("examples/new", user, attributes);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}

	@Test
	public void invalideCategory() throws ConfigurationException,
			ValidationException {

		Map<String, Object> attributes = createAttributes("category",
				"UnknownCategory");
		Request request = createGetRequest("new", user, attributes);
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));

	}

	@Test
	public void deleteRedirectsToParentResourceIfAllGone()
			throws ValidationException, ConfigurationException {

		String projectName = "deleteRedirectsToParentResourceIfAllGoneParent";
		String moduleName = projectName
				+ "/deleteRedirectsToParentResourceIfAllGoneModule";

		Module[] modules = createProjectAndImageModules(projectName, moduleName);

		Request request = createDeleteRequest(moduleName);

		try {
			Response response = executeRequest(request);
			assertThat(response.getStatus(), is(Status.SUCCESS_NO_CONTENT));
			assertThat(response.getLocationRef().getPath(),
					is("/" + Module.constructResourceUri(projectName)));
		} finally {
			try {
				for (Module module : modules) {
					module.remove();
				}
			} catch (NullPointerException ex) {
			}
		}
	}

	@Test
	public void deleteRedirectsToLatestResourceIfPreviousVersionsExists()
			throws ValidationException, ConfigurationException {

		String projectName = "deleteRedirectsToParentResourceIfAllGoneParent";
		String moduleName = projectName
				+ "/deleteRedirectsToParentResourceIfAllGoneModule";

		Module[] modules = createProjectAndImageModules(projectName, moduleName);

		// Create and store again to create a second version
		ImageModule image = new ImageModule(moduleName);
		image.setAuthz(new Authz(user.getName(), image));
		image = image.store();

		Module project = (Module) modules[1];

		Request request = createDeleteRequest(moduleName);

		try {
			Response response = executeRequest(request);
			assertThat(response.getStatus(), is(Status.SUCCESS_NO_CONTENT));
			assertThat(response.getLocationRef().getPath(), startsWith("/"
					+ Module.constructResourceUri(image.getName())));
		} finally {
			try {
				ImageModule.loadLatest(image.getId()).remove();
				project.remove();
			} catch (NullPointerException ex) {

			}
		}
	}

	private Module[] createProjectAndImageModules(String projectName,
			String moduleName) throws ValidationException {
		Module parent = new ProjectModule(projectName);
		parent.setAuthz(new Authz(user.getName(), parent));
		parent = parent.store();

		ImageModule image = new ImageModule(moduleName);
		image.setAuthz(new Authz(user.getName(), image));
		image = image.store();

		Module[] modules = { image, parent };
		return modules;
	}

	private Request createGetRequest(String module, User user,
			Map<String, Object> attributes) throws ConfigurationException,
			ValidationException {
		attributes.putAll(createModuleAttributes(module));
		Request request = createGetRequest(attributes);
		addUserToRequest(user, request);
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

	protected Request createDeleteRequest(String module)
			throws ConfigurationException, ValidationException {
		return createDeleteRequest(createRequestAttributes(module), user);
	}

	protected Map<String, Object> createRequestAttributes(String module) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("module", module);
		return attributes;
	}

}
