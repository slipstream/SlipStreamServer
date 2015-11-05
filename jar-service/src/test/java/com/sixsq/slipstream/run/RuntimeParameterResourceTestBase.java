package com.sixsq.slipstream.run;

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

import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.persistence.Package;
import com.sixsq.slipstream.util.CommonTestUtil;
import com.sixsq.slipstream.util.ResourceTestBase;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RuntimeParameterResourceTestBase extends ResourceTestBase {

	static protected ImageModule baseImage = null;

	public static void classSetup() throws ValidationException {
		baseImage = new ImageModule("RuntimeParameterResourceTestBaseImage");
		baseImage.setImageId("1234", cloudServiceName);
		baseImage.setIsBase(true);
		baseImage.store();

		user = CommonTestUtil.createTestUser();

		CommonTestUtil.addSshKeys(user);

		Event.muteForTests();
	}

	public static void classTearDown() throws ValidationException {
		baseImage.remove();
		try {
			CommonTestUtil.deleteUser(user);
		} catch (Exception ex) {
			// ok ... FIXME
		}
	}

	public void tearDown() {
		try {
			image.remove();
		} catch (Exception ex) {

		}

		try {
			deployment.remove();
		} catch (Exception ex) {

		}

	}

	protected Run createAndStoreRunWithRuntimeParameter(String moduleName,
			String key, String value) throws SlipStreamException,
			FileNotFoundException, IOException {
		Run run = createAndStoreRunImage(moduleName);
		run.assignRuntimeParameter(key, value, "");
		run.store();
		return run;
	}

	protected Response executeDecrementRequest(String key, Run run)
			throws ConfigurationException, ValidationException {
		Form form = new Form();
		form.add("decrement", "true");

		Request request = createPostRequest(run.getUuid(), key,
				form.getWebRepresentation());
		Response response = executeRequest(request);
		return response;
	}

	protected String executeGetRequestAndAssertValueSet(Run run, String key)
			throws ConfigurationException, ValidationException {
		Request request = createGetRequest(run.getUuid(), key);
		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_OK, response.getStatus());
		return response.getEntityAsText();
	}

	protected void executeGetRequestAndAssertValue(Run run, String key,
			String expectedValue) throws ConfigurationException,
			ValidationException {
		String actualValue = executeGetRequestAndAssertValueSet(run, key);
		assertEquals(expectedValue, actualValue);
	}

	protected Run createAndStoreRunImage(String moduleName)
			throws FileNotFoundException, IOException, SlipStreamException {
		image = new ImageModule(moduleName);
		image.setModuleReference(baseImage);
		image.getPackages().add(new Package("package1"));
		image.setModuleReference(baseImage);
		image.setImageId("image-id", cloudServiceName);
		image = image.store();

		Run run = RunFactory.getRun(image, RunType.Run, user);
		run.setParameter(new RunParameter("foo", "bar", "baz"));
		return (Run) run.store();
	}

	protected Request createGetRequest(String uuid, String key)
			throws ConfigurationException, ValidationException {
		return createGetRequest(createRequestAttributes(uuid, key));
	}

	protected Request createPutRequest(String uuid, String key,
			Representation entity) throws ConfigurationException,
			ValidationException {
		return createPutRequest(createRequestAttributes(uuid, key), entity);
	}

	protected Request createDeleteRequest(String uuid, String key)
			throws ConfigurationException, ValidationException {
		return createDeleteRequest(createRequestAttributes(uuid, key));
	}

	protected Request createPostRequest(String uuid, String key,
			Representation entity) throws ConfigurationException,
			ValidationException {
		return createPostRequest(createRequestAttributes(uuid, key), entity);
	}

	protected Map<String, Object> createRequestAttributes(String uuid,
			String key) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("uuid", uuid);
		attributes.put("key", key);
		return attributes;
	}

	protected Response executeRequest(Request request) {
		return executeRequest(request, new RuntimeParameterResource());
	}

}
