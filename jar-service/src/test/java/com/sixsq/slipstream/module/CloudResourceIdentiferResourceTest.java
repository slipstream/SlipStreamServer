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

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserTest;
import com.sixsq.slipstream.util.ResourceTestBase;
import org.junit.*;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CloudResourceIdentiferResourceTest extends ResourceTestBase {

	protected static User user = UserTest.createUser("test", UserTest.PASSWORD);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		UserTest.storeUser(user);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void getCloudResourceId() throws ConfigurationException,
			ValidationException {

		String moduleUri = "getCloudResourceId";
		ImageModule module = new ImageModule(moduleUri);
		module.store();

		String cloudServiceName = "cloudservicex";
		String cloudImageIdentifer = "1234";
		CloudImageIdentifier cloudImageIdentifier = new CloudImageIdentifier(
				module, cloudServiceName);
		cloudImageIdentifier.setCloudMachineIdentifer(cloudImageIdentifer);
		cloudImageIdentifier.store();

		Request request = createGetRequest(module, cloudServiceName);

		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_OK, response.getStatus());
		assertEquals(cloudImageIdentifer, response.getEntityAsText());

		module.remove();
	}

	@Test
	public void getCloudWrongModule() throws ConfigurationException,
			ValidationException {

		Request request = createGetRequest("moduledoesntexists", 123,
				"whatever");

		Response response = executeRequest(request);

		assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
	}

	@Test
	public void putCloudImageIdNoRegion() throws ValidationException,
			ConfigurationException {
		String moduleUri = "putCloudImageId";
		Module module = new ImageModule(moduleUri);
		module.store();

		String cloudServiceName = "cloudservicex";
		String cloudImageIdentifer = "1234";

		Request request = createPutRequest(module, cloudServiceName, "",
				cloudImageIdentifer);

		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_CREATED, response.getStatus());

		CloudImageIdentifier id = CloudImageIdentifier.load(module
				.getResourceUri() + "/" + cloudServiceName);
		assertEquals(cloudImageIdentifer, id.getCloudMachineIdentifer());

		module.remove();
	}

	@Test
	public void putCloudImageIdWithRegion() throws ValidationException,
			ConfigurationException {
		String moduleUri = "putCloudImageId";
		Module module = new ImageModule(moduleUri);
		module.store();

		String cloudServiceName = "cloudservicex";
		String region = "region-a";
		String cloudImageIdentifer = "1234";

		Request request = createPutRequest(module, cloudServiceName, region,
				cloudImageIdentifer);

		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_CREATED, response.getStatus());

		CloudImageIdentifier id = CloudImageIdentifier.load(module
				.getResourceUri()
				+ "/"
				+ cloudServiceName
				+ CloudImageIdentifier.CLOUD_SERVICE_ID_SEPARATOR + region);
		assertEquals(cloudImageIdentifer, id.getCloudMachineIdentifer());

		module.remove();
	}

	@Test
	public void putExistingCloudImageIdFails() throws ValidationException,
			ConfigurationException {
		String moduleUri = "putExistingCloudImageIdFails";
		ImageModule module = new ImageModule(moduleUri);
		module.store();

		String cloudServiceName = "cloudservicex";
		String cloudImageIdentifer = "1234";

		CloudImageIdentifier cloudImageIdentifier = new CloudImageIdentifier(
				module, cloudServiceName);
		cloudImageIdentifier.setCloudMachineIdentifer(cloudImageIdentifer);
		cloudImageIdentifier.store();

		Request request = createPutRequest(module, cloudServiceName, null,
				cloudImageIdentifer);

		Response response = executeRequest(request);

		assertEquals(Status.CLIENT_ERROR_FORBIDDEN, response.getStatus());

		CloudImageIdentifier id = CloudImageIdentifier.load(module
				.getResourceUri() + "/" + cloudServiceName);
		assertEquals(cloudImageIdentifer, id.getCloudMachineIdentifer());

		module.remove();
	}

	protected Request createGetRequest(Module module, String cloudServiceName)
			throws ConfigurationException, ValidationException {
		return createGetRequest(module.getName(), module.getVersion(),
				cloudServiceName);
	}

	protected Request createGetRequest(String moduleUri, int version,
			String cloudResourceUri) throws ConfigurationException,
			ValidationException {
		Request request = createGetRequest(createModuleAttributes(moduleUri));
		Map<String, Object> attributes = createAttributes("cloudservice",
				cloudResourceUri);
		attributes.put("version", version);
		request.getAttributes().putAll(attributes);
		// TODO CookieUtils.addAuthnCookie(request, user.getName());
		return request;
	}

	protected Request createPutRequest(Module module, String cloudServiceName,
			String region, String value) throws ConfigurationException,
			ValidationException {
		return createPutRequest(module.getName(), module.getVersion(),
				cloudServiceName, region, value);
	}

	protected Request createPutRequest(String moduleUri, int version,
			String cloudServiceName, String region, String value)
			throws ConfigurationException, ValidationException {
		Representation entity = new StringRepresentation(value);
		Request request = createPutRequest(createModuleAttributes(moduleUri),
				entity);
		Map<String, Object> attributes = createAttributes("cloudservice",
				cloudServiceName);
		attributes.put("version", version);
		if (region != null) {
			attributes.put("region", region);
		}
		request.getAttributes().putAll(attributes);
		// TODO CookieUtils.addAuthnCookie(request, user.getName());
		return request;
	}

	private Response executeRequest(Request request) {
		return executeRequest(request, new CloudResourceIdentifierResource());
	}

}
