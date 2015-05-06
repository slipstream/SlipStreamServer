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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import com.sixsq.slipstream.persistence.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.connector.local.LocalUserParametersFactory;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.user.UserTest;
import com.sixsq.slipstream.util.ResourceTestBase;
import com.sixsq.slipstream.util.XmlUtil;

public class RunListResourceTest extends ResourceTestBase {

	private static final String NODE_NAME = "n1";

	private static final String PARAMETER_NAME = "parameter_name";

	static protected ImageModule baseImage = null;

	@BeforeClass
	public static void setupClass() throws ValidationException,
			ConfigurationException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {

		resetAndLoadConnector(com.sixsq.slipstream.connector.local.LocalConnector.class);

		try {
			user = UserTest.storeUser(user);
		} catch (Exception ex) {

		}

	}

	private static void removeAllRuns() throws ConfigurationException, ValidationException {
		for (Run r : Run.listAll()) {
			try {
				r.remove();
			} catch (Exception ex) {

			}
		}
	}

	@AfterClass
	public static void teardownClass() throws ConfigurationException,
			ValidationException {

		removeAllRuns();

	}

	@Before
	public void setup() throws ValidationException, ConfigurationException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {

		baseImage = new ImageModule("RuntimeParameterResourceTestBaseImage");
		baseImage.setImageId("1234", cloudServiceName);
		baseImage.setIsBase(true);
		baseImage = baseImage.store();

		user = User.loadByName(user.getName());

		createModules("overrideParmeters");

	}

	@After
	public void tearDown() throws ValidationException {
		baseImage.remove();

		try {
			image.remove();
		} catch (Exception ex) {

		}

		try {
			deployment.remove();
		} catch (Exception ex) {

		}

	}

	@Test
	public void testPaging() throws ValidationException, SAXException, ParserConfigurationException, IOException {
		removeAllRuns();

		Set<ConnectorInstance> cloudServicesA = new HashSet<ConnectorInstance>();
		cloudServicesA.add(new ConnectorInstance("CloudA", null));
		Set<ConnectorInstance> cloudServicesAB = new HashSet<ConnectorInstance>();
		cloudServicesAB.add(new ConnectorInstance("CloudA", null));
		cloudServicesAB.add(new ConnectorInstance("CloudB", null));
		Set<ConnectorInstance> cloudServicesABC = new HashSet<ConnectorInstance>();
		cloudServicesABC.add(new ConnectorInstance("CloudA", null));
		cloudServicesABC.add(new ConnectorInstance("CloudB", null));
		cloudServicesABC.add(new ConnectorInstance("CloudC", null));

		Run run = new Run(deployment, RunType.Orchestration, cloudServicesA, user).store();
		run = new Run(deployment, RunType.Orchestration, cloudServicesAB, user).store();
		run = new Run(deployment, RunType.Orchestration, cloudServicesABC, user).store();
		run = new Run(deployment, RunType.Orchestration, cloudServicesABC, user).store();
		run = new Run(deployment, RunType.Orchestration, cloudServicesABC, user).store();

		Response resp = getRunList(null, null, null);
		assertEquals(Status.SUCCESS_OK, resp.getStatus());
		Document runs = XmlUtil.stringToDom(resp.getEntityAsText());
		assertEquals(5, runs.getDocumentElement().getElementsByTagName("item").getLength());
		assertEquals("5", runs.getDocumentElement().getAttribute("count"));
		assertEquals("5", runs.getDocumentElement().getAttribute("totalCount"));
		assertEquals("0", runs.getDocumentElement().getAttribute("offset"));
		assertEquals("20", runs.getDocumentElement().getAttribute("limit"));

		resp = getRunList(null, 10, "CloudB");
		assertEquals(Status.SUCCESS_OK, resp.getStatus());
		String xml = resp.getEntityAsText();
		runs = XmlUtil.stringToDom(xml);
		assertEquals(4, runs.getDocumentElement().getElementsByTagName("item").getLength());
		assertEquals("4", runs.getDocumentElement().getAttribute("count"));
		assertEquals("4", runs.getDocumentElement().getAttribute("totalCount"));
		assertEquals("10", runs.getDocumentElement().getAttribute("limit"));

		resp = getRunList(1, 4, null);
		assertEquals(Status.SUCCESS_OK, resp.getStatus());
		runs = XmlUtil.stringToDom(resp.getEntityAsText());
		assertEquals(4, runs.getDocumentElement().getElementsByTagName("item").getLength());
		assertEquals("4", runs.getDocumentElement().getAttribute("count"));
		assertEquals("5", runs.getDocumentElement().getAttribute("totalCount"));
		assertEquals("1", runs.getDocumentElement().getAttribute("offset"));
		assertEquals("4", runs.getDocumentElement().getAttribute("limit"));

		resp = getRunList(null, null, "CloudC");
		assertEquals(Status.SUCCESS_OK, resp.getStatus());
		runs = XmlUtil.stringToDom(resp.getEntityAsText());
		assertEquals(3, runs.getDocumentElement().getElementsByTagName("item").getLength());
		assertEquals("3", runs.getDocumentElement().getAttribute("count"));
		assertEquals("3", runs.getDocumentElement().getAttribute("totalCount"));

		resp = getRunList(-1, null, null);
		assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, resp.getStatus());

		resp = getRunList(null, 10000 + 1, null);
		assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, resp.getStatus());
	}

	@Test
	@Ignore
	public void overrideParameters() throws ConfigurationException,
			NotFoundException, AbortException, ValidationException {
		int multiplicityOverride = 2;
		String overrideValue = "another value";

		UserParameter keyParameter = new UserParameter(
				new LocalUserParametersFactory()
						.constructKey(LocalUserParametersFactory.KEY_PARAMETER_NAME));
		user.setParameter(keyParameter);
		UserParameter secretParameter = new UserParameter(
				new LocalUserParametersFactory()
						.constructKey(LocalUserParametersFactory.SECRET_PARAMETER_NAME));
		user.setParameter(secretParameter);
		user.setDefaultCloudServiceName(LocalConnector.CLOUD_SERVICE_NAME);
		user = user.store();

		List<NodeParameter> override = new ArrayList<NodeParameter>();
		NodeParameter parameter = new NodeParameter(PARAMETER_NAME);
		parameter.setUnsafeValue(overrideValue);
		override.add(parameter);

		Request request = createPostRequest(override, multiplicityOverride,
				deployment.getResourceUri());
		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_CREATED, response.getStatus());

		assertMultiplicityOverride(multiplicityOverride, response);
		assertParameterOverride(PARAMETER_NAME, response, overrideValue);
	}

	private void assertMultiplicityOverride(Integer multiplicityOverride,
			Response response) {
		String multiplicityParameterName = NODE_NAME
				+ RuntimeParameter.NODE_MULTIPLICITY_INDEX_SEPARATOR
				+ multiplicityOverride.toString()
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
				+ RuntimeParameter.MULTIPLICITY_PARAMETER_NAME;

		String uuid = response.getLocationRef().getLastSegment();

		RuntimeParameter multiplicity = RuntimeParameter.loadFromUuidAndKey(
				uuid, multiplicityParameterName);

		assertThat(multiplicity.getValue(),
				is(String.valueOf(multiplicityOverride.toString())));
	}

	private void assertParameterOverride(String parameterName,
			Response response, String overrideValue) {
		String processedParameterName = NODE_NAME
				+ RuntimeParameter.NODE_MULTIPLICITY_INDEX_SEPARATOR
				+ RuntimeParameter.MULTIPLICITY_NODE_START_INDEX
				+ RuntimeParameter.NODE_PROPERTY_SEPARATOR + parameterName;

		String uuid = response.getLocationRef().getLastSegment();

		RuntimeParameter parameter = RuntimeParameter.loadFromUuidAndKey(uuid,
				processedParameterName);

		assertThat(parameter.getValue(), is(overrideValue));
	}

	private void createModules(String moduleName) throws ValidationException {
		image = new ImageModule(moduleName);
		image.setModuleReference(baseImage);
		image.setImageId("123", cloudServiceName);

		image.setParameter(new ModuleParameter(PARAMETER_NAME, "default value",
				""));
		image.setImageId("abc", LocalConnector.CLOUD_SERVICE_NAME);

		image = image.store();

		Node node = new Node(NODE_NAME, image);
		node.setMultiplicity(1);
		node.setParameter(new NodeParameter(PARAMETER_NAME, "'default value'"));

		deployment = new DeploymentModule(moduleName + "Deployment");
		deployment.setNode(node);
		deployment = (DeploymentModule) deployment.store();
	}

	private Request createPostRequest(List<NodeParameter> parameters,
			int multiplicity, String moduleUri) throws ConfigurationException,
			ValidationException {
		Form form = createRunForm(parameters, multiplicity);
		form.add(RunListResource.REFQNAME, moduleUri);

		Request request = createPostRequest(form.getWebRepresentation());
		addUserToRequest(user, request);
		return request;
	}

	private Form createRunForm(List<NodeParameter> parameters,
			Integer muliplicity) {
		Form form = new Form();
		for (NodeParameter np : parameters) {
			String name = buildNodeName(NODE_NAME, np.getName());
			String value = np.getValue();
			form.add(name, value);
		}
		form.add(
				buildNodeName(NODE_NAME,
						RuntimeParameter.MULTIPLICITY_PARAMETER_NAME),
				muliplicity.toString());

		return form;
	}

	protected String buildNodeName(String nodeName, String parameterName) {
		return "parameter--node--" + nodeName + "--" + parameterName;
	}

	private Request createPostRequest(Representation entity)
			throws ConfigurationException, ValidationException {
		return createPostRequest(new HashMap<String, Object>(), entity);
	}

	private Response getRunList(Integer offset, Integer limit, String cloudServiceName)
			throws ConfigurationException, ValidationException {

		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(User.REQUEST_KEY, user);

		Form queryString = new Form();
		if (offset != null) queryString.set("offset", offset.toString());
		if (limit != null) queryString.set("limit", limit.toString());
		if (cloudServiceName != null) queryString.set("cloud", cloudServiceName);

		Request req = createGetRequest("?" + queryString.getQueryString(), attributes);
		return executeRequest(req);
	}

	private Response executeRequest(Request request) {
		return executeRequest(request, new RunListResource());
	}

}
