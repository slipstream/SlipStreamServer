package com.sixsq.slipstream.util;

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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.sixsq.slipstream.es.CljElasticsearchHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.sixsq.slipstream.authn.AuthenticatorBase;
import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.SystemConfigurationParametersFactoryBase;
import com.sixsq.slipstream.connector.local.LocalConnector;
import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Authz;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfiguration.RequiredParameters;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.run.RunTestBase;
import com.sixsq.slipstream.persistence.UserTest;

public class ResourceTestBase extends RunTestBase {

	@BeforeClass
	public static void createTestElasticsearchDb(){
		CljElasticsearchHelper.createAndInitTestDb();
	}

	@AfterClass
	public static void stopTestElasticsearchDb(){
		CljElasticsearchHelper.stopAndUnbindTestDb();
	}

	protected static final String TEST_REQUEST_NAME = "/test/request";

	// Need to set cloudServiceName before the status user is
	// created, since the createUser method uses it
	public static String cloudServiceName = new LocalConnector()
			.getCloudServiceName();

	protected static User user = UserTest.createUser("test", UserTest.PASSWORD);

	public static void resetAndLoadConnector(
			Class<? extends Connector> connectorClass)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {

		// Instantiate the configuration force loading from disk
		// This is required otherwise the first loading from
		// disk will reset the connectors
		try {
			Configuration.getInstance();
		} catch (ConfigurationException e) {
			fail();
		} catch (ValidationException e) {
			fail();
		}

		Map<String, Connector> connectors = new HashMap<String, Connector>();
		Connector connector = ConnectorFactory
				.instantiateConnectorFromName(connectorClass.getName());
		connectors.put(connector.getCloudServiceName(), connector);
		ConnectorFactory.setConnectors(connectors);
	}

	public Request createRequest(Map<String, Object> attributes, Method method)
			throws ConfigurationException, ValidationException {
		return createRequest(attributes, method, null);
	}

	public Request createRequest(Map<String, Object> attributes, Method method,
			Representation entity) throws ConfigurationException,
			ValidationException {
		return createRequest(attributes, method, entity, null);
	}

	public Request createRequest(Map<String, Object> attributes, Method method,
			Representation entity, String targetUrl)
			throws ConfigurationException, ValidationException {
		return createRequest(attributes, method, entity, targetUrl, null);
	}

	public Request createRequest(Method method, Cookie cookie)
			throws ConfigurationException, ValidationException {
		return createRequest(null, method, null, null, cookie);
	}

	public Request createRequest(Method method, String targetUrl, Cookie cookie)
			throws ConfigurationException, ValidationException {
		return createRequest(null, method, null, targetUrl, cookie);
	}

	public Request createRequest(Map<String, Object> attributes, Method method,
			Representation entity, String targetUrl, Cookie cookie)
			throws ConfigurationException, ValidationException {
		if (targetUrl == null) {
			targetUrl = TEST_REQUEST_NAME;
		}

		Request request = new Request(method, "http://something.org"
				+ targetUrl);
		request.setRootRef(new Reference("http://something.org"));

		if (attributes != null) {
			request.setAttributes(attributes);
		}
		if (entity != null) {
			request.setEntity(entity);
		}
		if (cookie != null) {
			Series<Cookie> cookies = new Series<Cookie>(Cookie.class);
			cookies.add(cookie);
			request.setCookies(cookies);
		}

		try {
			ConfigurationUtil.addConfigurationToRequest(request);
		} catch (ValidationException e) {
			fail();
		}

		if (cookie != null) {
			String username = CookieUtils.getCookieUsername(cookie);
			User user = User.loadByName(username);
			AuthenticatorBase.setUserInRequest(user, request);
		}

		return request;
	}

	protected Request createGetRequest(Map<String, Object> attributes)
			throws ConfigurationException, ValidationException {
		Method method = Method.GET;
		return createRequest(attributes, method);
	}

	protected Request createGetRequest(String targetUrl, Map<String, Object> attributes)
			throws ConfigurationException, ValidationException {
		Method method = Method.GET;
		return createRequest(attributes, method, null, targetUrl);
	}

	protected Request createPutRequest(Map<String, Object> attributes,
			Representation entity) throws ConfigurationException,
			ValidationException {
		return createRequest(attributes, Method.PUT, entity);
	}

	protected Request createPutRequest(Map<String, Object> attributes,
			Representation entity, String targetUrl)
			throws ConfigurationException, ValidationException {
		return createRequest(attributes, Method.PUT, entity, targetUrl);
	}

	protected Request createDeleteRequest(Map<String, Object> attributes)
			throws ConfigurationException, ValidationException {
		Method method = Method.DELETE;
		return createRequest(attributes, method);
	}

	protected Request createDeleteRequest(Map<String, Object> attributes,
			User user) throws ConfigurationException, ValidationException {
		Method method = Method.DELETE;
		Request request = createRequest(attributes, method);
		addUserToRequest(user, request);
		return request;
	}

	protected Request createPostRequest(Map<String, Object> attributes,
			Representation entity) throws ConfigurationException,
			ValidationException {
		Method method = Method.POST;
		return createRequest(attributes, method, entity);
	}

	protected Response executeRequest(Request request, ServerResource resource) {

		Response response = new Response(request);

		resource.init(null, request, response);
		if (response.getStatus().isSuccess()) {
			resource.handle();
		}

		return resource.getResponse();
	}

	protected Map<String, Object> createAttributes(String name, String value) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(name, value);
		return attributes;
	}

	protected Request createGetRequest(String module, User user)
			throws ConfigurationException, ValidationException {
		Request request = createGetRequest(createModuleAttributes(module));
		addUserToRequest(user, request);
		return request;
	}

	protected Module createAndStoreProject(String projectName)
			throws ValidationException {
		Module project = new ProjectModule(projectName);
		project.setAuthz(new Authz(user.getName(), project));
		project.store();
		return project;
	}

	protected Request createPutRequest(String name, Representation entity,
			User user, String targetUrl) throws ConfigurationException,
			ValidationException {
		Request request = createPutRequest(createModuleAttributes(name),
				entity, targetUrl);
		addUserToRequest(user, request);
		return request;
	}

	protected Request createPutRequest(String name, Representation entity,
			User user) throws ConfigurationException, ValidationException {
		Request request = createPutRequest(createModuleAttributes(name), entity);
		addUserToRequest(user, request);
		return request;
	}

	protected Map<String, Object> createModuleAttributes(String module) {
		return createAttributes("module", module);
	}

	protected void addUserToRequest(String username, Request request)
			throws ConfigurationException, ValidationException {
		addUserToRequest(User.loadByName(username), request);
	}

	protected void addUserToRequest(User user, Request request) {
		request.getClientInfo().setUser(
				new org.restlet.security.User(user.getName()));
		AuthenticatorBase.setUserInRequest(user, request);
	}

	public static void setCloudConnector(String connectorClassName)
			throws ConfigurationException {
		Configuration configuration = null;
		try {
			configuration = Configuration.getInstance();
		} catch (ValidationException e) {
			fail();
		}

		ServiceConfiguration sc = configuration.getParameters();
		try {
			sc.setParameter(new ServiceConfigurationParameter(
					RequiredParameters.CLOUD_CONNECTOR_CLASS.getName(),
					connectorClassName));
		} catch (ValidationException e) {
			fail();
		}
		sc.store();
		ConnectorFactory.resetConnectors();
	}

	protected static void updateServiceConfigurationParameters(
			SystemConfigurationParametersFactoryBase connectorSystemConfigFactory)
			throws ValidationException {
		ServiceConfiguration sc = Configuration.getInstance().getParameters();
		sc.setParameters(connectorSystemConfigFactory.getParameters());
		sc.store();
	}
}
