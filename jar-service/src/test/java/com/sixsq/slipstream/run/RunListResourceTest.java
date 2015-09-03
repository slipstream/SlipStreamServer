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

import com.sixsq.slipstream.connector.ExecutionControlUserParametersFactory;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.connector.local.LocalUserParametersFactory;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.util.ResourceTestBase;
import com.sixsq.slipstream.util.XmlUtil;
import org.junit.*;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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

		UserParameter keyParameter = new UserParameter(
				new LocalUserParametersFactory()
						.constructKey(LocalUserParametersFactory.KEY_PARAMETER_NAME), "value", "desc");
		user.setParameter(keyParameter);

        UserParameter secretParameter = new UserParameter(
                new LocalUserParametersFactory()
                        .constructKey(LocalUserParametersFactory.SECRET_PARAMETER_NAME), "value", "desc");
        user.setParameter(secretParameter);


		Event.muteForTests();

        UserParameter publicKey = new UserParameter(ExecutionControlUserParametersFactory.CATEGORY +
                "." + UserParameter.SSHKEY_PARAMETER_NAME, "value", "desc");
        user.setParameter(publicKey);

		user.setDefaultCloudServiceName(LocalConnector.CLOUD_SERVICE_NAME);
		user = user.store();

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
		baseImage.setAuthz(new Authz(user.getName(),baseImage));
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
	public void testPagination() throws ValidationException, SAXException, ParserConfigurationException, IOException {
		removeAllRuns();

		Set<String> cloudServiceNamesA = new HashSet<String>();
		cloudServiceNamesA.add("CloudA");
		Set<String> cloudServiceNamesB = new HashSet<String>();
		cloudServiceNamesB.add("CloudA");
		cloudServiceNamesB.add("CloudB");
		Set<String> cloudServiceNamesC = new HashSet<String>();
		cloudServiceNamesC.add("CloudA");
		cloudServiceNamesC.add("CloudB");
		cloudServiceNamesC.add("CloudC");

		(new Run(deployment, RunType.Orchestration, cloudServiceNamesA, user)).store();
		(new Run(deployment, RunType.Orchestration, cloudServiceNamesB, user)).store();
		(new Run(deployment, RunType.Orchestration, cloudServiceNamesC, user)).store();
		(new Run(deployment, RunType.Orchestration, cloudServiceNamesC, user)).store();
		(new Run(deployment, RunType.Orchestration, cloudServiceNamesC, user)).store();

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
		runs = XmlUtil.stringToDom(resp.getEntityAsText());
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
	public void createImageRun() throws ConfigurationException,
			NotFoundException, AbortException, ValidationException {

        Form form = new Form();
        form.add(RunListResource.REFQNAME, baseImage.getResourceUri());

        Request request = createPostRequest(form.getWebRepresentation());
		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_CREATED, response.getStatus());
		assertThat(response.getLocationRef().getPath(), startsWith("/run/"));
	}

	@Test
	public void createDeploymentRun() throws ConfigurationException,
			NotFoundException, AbortException, ValidationException {

        Form form = new Form();
        form.add(RunListResource.REFQNAME, deployment.getResourceUri());

        Request request = createPostRequest(form.getWebRepresentation());

		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_CREATED, response.getStatus());
		assertThat(response.getLocationRef().getPath(), startsWith("/run/"));
	}

	@Test
	public void createImageRunWithRedirect() throws ConfigurationException,
			NotFoundException, AbortException, ValidationException {

        Form form = new Form();
        form.add(RunListResource.REFQNAME, baseImage.getResourceUri());
        form.add(RunListResource.REDIRECT_TO_DASHBOARD, "true");

        Request request = createPostRequest(form.getWebRepresentation());

		Response response = executeRequest(request);

		assertEquals(Status.SUCCESS_CREATED, response.getStatus());
		assertThat(response.getLocationRef().getPath(), is("/dashboard"));
	}

	private Request createPostRequest(String moduleUri) throws ConfigurationException,
			ValidationException {
		Form form = new Form();
		form.add(RunListResource.REFQNAME, moduleUri);

		Request request = createPostRequest(form.getWebRepresentation());
		addUserToRequest(user, request);
		return request;
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

		image.setAuthz(new Authz(user.getName(), image));

		image = image.store();

		Node node = new Node(NODE_NAME, image);
		node.setMultiplicity(1);
		node.setParameter(new NodeParameter(PARAMETER_NAME, "'default value'"));

		deployment = new DeploymentModule(moduleName + "Deployment");
		deployment.setNode(node);
		deployment.setAuthz(new Authz(user.getName(), deployment));
		deployment = (DeploymentModule) deployment.store();
	}

	private Request createPostRequest(List<NodeParameter> parameters,
			int multiplicity, String moduleUri) throws ConfigurationException,
			ValidationException {
		Form form = createRunForm(parameters, multiplicity);
		form.add(RunListResource.REFQNAME, moduleUri);

		return createPostRequest(form.getWebRepresentation());
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
        Request request = createPostRequest(new HashMap<String, Object>(), entity);
        addUserToRequest(user, request);
        return request;
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
