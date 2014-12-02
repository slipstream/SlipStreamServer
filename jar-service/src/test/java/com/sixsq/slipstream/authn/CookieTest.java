package com.sixsq.slipstream.authn;

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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfiguration.RequiredParameters;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.run.RunResource;
import com.sixsq.slipstream.run.RuntimeParameterResource;
import com.sixsq.slipstream.user.UserResource;
import com.sixsq.slipstream.user.UserTest;
import com.sixsq.slipstream.util.CommonTestUtil;
import com.sixsq.slipstream.util.ResourceTestBase;
import com.sixsq.slipstream.util.SerializationUtil;
import com.sixsq.slipstream.util.XmlUtil;

public class CookieTest extends ResourceTestBase {

	protected static Run runA = null;
	protected static Run runB = null;
	protected static Cookie cookieStd = null;
	protected static Cookie cookieRunA = null;
	protected static Cookie cookieRunB = null;
	protected static File publicKeyFile = null;
	protected static String cloudServiceNameB = "cloudB";

	@Before
	public void setUpBeforeClass() throws Exception {
		createAndStoreUser();
		setupDeployments();
		createPublicKeyFileAndSetConnector();
		createAndStoreRuns();
		cookieRunA = createCookie(user, runA);
		cookieRunB = createCookie(user, runB);
		cookieStd = new Cookie(CookieUtils.COOKIE_NAME,
				CookieUtils.createCookie(user.getName(), cloudServiceName));
	}

	private void createAndStoreUser() throws NoSuchAlgorithmException,
			UnsupportedEncodingException, ConfigurationException,
			ValidationException {

		user = CommonTestUtil.createUser("test");
		user.hashAndSetPassword("password");

		UserParameter userParam = new UserParameter(cloudServiceName + "."
				+ UserParametersFactoryBase.SECRET_PARAMETER_NAME, "pass", "");
		userParam.setCategory(cloudServiceName);
		user.setParameter(userParam);

		userParam = new UserParameter(cloudServiceNameB + "."
				+ UserParametersFactoryBase.SECRET_PARAMETER_NAME, "pass", "");
		userParam.setCategory(cloudServiceNameB);
		user.setParameter(userParam);

		user.store();
	}

	private void createPublicKeyFileAndSetConnector() throws IOException,
			ValidationException {
		Map<String, ServiceConfigurationParameter> params = new HashMap<String, ServiceConfigurationParameter>(
				1);

		publicKeyFile = File.createTempFile("tempfile", ".tmp");
		String paramName = ServiceConfiguration.CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY;
		ServiceConfigurationParameter param = new ServiceConfigurationParameter(
				paramName, publicKeyFile.getPath());
		params.put(paramName, param);

		paramName = RequiredParameters.CLOUD_CONNECTOR_CLASS.getName();
		param = new ServiceConfigurationParameter(paramName, cloudServiceName
				+ ":com.sixsq.slipstream.connector.local.LocalConnector,"
				+ cloudServiceNameB
				+ ":com.sixsq.slipstream.connector.local.LocalConnector");
		params.put(paramName, param);

		Configuration.getInstance().update(params);
	}

	private void createAndStoreRuns() throws SlipStreamClientException {
		runA = RunFactory.getRun(deployment, RunType.Orchestration, user);
		runA.store();
		runB = RunFactory.getRun(deployment, RunType.Orchestration, user);
		runB.store();
	}

	@After
	public void tearDownAfterClass() throws Exception {
		user.remove();
		runA.remove();
		runB.remove();
	}

	@Test
	public void checkMachineCookieBindToOneRun() throws ConfigurationException, ValidationException {

		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put("uuid", runA.getUuid());

		Request request = createRequest(attributes, Method.GET, null, null, cookieRunA);

		request.setClientInfo(getClientInfo());
		Response response = executeRequest(request, new RunResource());

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

		attributes.put("uuid", runB.getUuid());
		request = createRequest(attributes, Method.GET, null, null, cookieRunA);

		request.setClientInfo(getClientInfo());
		response = executeRequest(request, new RunResource());

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));
	}

	@Test
	public void checkMachineCookieCloudCredentialsFiltering()
			throws ConfigurationException, ValidationException {

		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("user", user.getName());

		Request request = createRequest(attributes, Method.GET, null, null,
				cookieRunA);
		request.setClientInfo(getClientInfo());
		Response response = executeRequest(request, new UserResource());

		String keyA = cloudServiceName + "."
				+ UserParametersFactoryBase.SECRET_PARAMETER_NAME;
		String keyB = cloudServiceNameB + "."
				+ UserParametersFactoryBase.SECRET_PARAMETER_NAME;

		assertThat(response.getEntityAsText().contains(keyA), is(true));
		assertThat(response.getEntityAsText().contains(keyB), is(false));

		request = createRequest(attributes, Method.GET, null, null, cookieStd);
		request.setClientInfo(getClientInfo());
		response = executeRequest(request, new UserResource());

		assertThat(response.getEntityAsText().contains(keyA), is(true));
		assertThat(response.getEntityAsText().contains(keyB), is(true));
	}

	@Test
	public void checkMachineCookieWithRuntimeParams()
			throws ConfigurationException, ValidationException {

		String value = "http://sixsq.com/";

		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("key", RuntimeParameter.GLOBAL_URL_SERVICE_KEY);
		attributes.put("uuid", runA.getUuid());

		Representation entity = new StringRepresentation(value);
		Request request = createRequest(attributes, Method.PUT, entity, null,
				cookieRunA);
		request.setClientInfo(getClientInfo());
		Response response = executeRequest(request,
				new RuntimeParameterResource());

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

		request = createRequest(attributes, Method.GET, null, null, cookieRunA);
		request.setClientInfo(getClientInfo());
		response = executeRequest(request, new RuntimeParameterResource());

		assertThat(response.getEntityAsText(), is(value));

		attributes.put("uuid", runB.getUuid());
		request = createRequest(attributes, Method.GET, null, null, cookieRunA);
		request.setClientInfo(getClientInfo());
		response = executeRequest(request, new RuntimeParameterResource());

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));

	}

	@Test
	public void checkStdCookieCanGetSetUserParam() throws ConfigurationException, ValidationException, SAXException,
			ParserConfigurationException, IOException {

		Response response = getAndPostUser(cookieStd);
		assertThat(response.getStatus(), is(Status.SUCCESS_ACCEPTED));
	}

	@Test
	public void ensureMachineCookieCanotSetUserParam() throws ConfigurationException, ValidationException,
			SAXException, ParserConfigurationException, IOException {

		Response response = getAndPostUser(cookieRunA);
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_FORBIDDEN));
	}

	private Response getAndPostUser(Cookie cookie) throws ConfigurationException, ValidationException, SAXException,
			ParserConfigurationException, IOException {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("user", user.getName());

		Request request = createRequest(attributes, Method.GET, null, null, cookie);
		request.setClientInfo(getClientInfo());
		Response response = executeRequest(request, new UserResource());

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));

		Document XmlDocument = XmlUtil.stringToDom(response.getEntityAsText());
		XmlDocument.getDocumentElement().setAttribute("password", UserTest.PASSWORD);
		String value = SerializationUtil.documentToString(XmlDocument);

		Representation entity = new StringRepresentation(value);
		entity.setMediaType(MediaType.APPLICATION_XML);
		request = createRequest(attributes, Method.PUT, entity, null, cookie);
		request.setClientInfo(getClientInfo());
		return executeRequest(request, new UserResource());
	}

	private Cookie createCookie(User user, Run run) {
		Properties extraProperties = new Properties();
		extraProperties.put(CookieUtils.COOKIE_IS_MACHINE, "true");
		extraProperties.put(CookieUtils.COOKIE_RUN_ID, run.getUuid());
		extraProperties.put(CookieUtils.COOKIE_EXPIRY_DATE, "0");

        String[] cloudServiceNames = run.getCloudServiceNamesList();
		String cloudName = (cloudServiceNames == null || cloudServiceNames.length == 0) ? cloudServiceName
		        : cloudServiceNames[0];

        String value = CookieUtils.createCookie(user.getName(), cloudName, extraProperties);

		return new Cookie(CookieUtils.COOKIE_NAME, value);
	}

	private ClientInfo getClientInfo() {
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setUser(new org.restlet.security.User(user.getName()));
		return clientInfo;
	}

}
