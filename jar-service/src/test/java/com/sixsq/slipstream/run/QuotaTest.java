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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.connector.dummy.DummyCloudCredDef;
import com.sixsq.slipstream.connector.dummy.DummyConnector;
import com.sixsq.slipstream.connector.dummy.DummySystemConfigurationParametersFactory;
import com.sixsq.slipstream.credentials.CloudCredentialCreateTmpl;
import com.sixsq.slipstream.es.CljElasticsearchHelper;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.QuotaException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.ssclj.app.SscljTestServer;
import com.sixsq.slipstream.util.CommonTestUtil;
import com.sixsq.slipstream.util.SscljProxy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.sixsq.slipstream.persistence.ServiceConfiguration;
import org.restlet.Response;

public class QuotaTest {

	private static final String USERNAME = "test";

	public static void setupBackend() {
		SscljTestServer.start();
		CljElasticsearchHelper.initTestDb();
	}

	public static void teardownBackend() {
		SscljTestServer.stop();
	}

	@BeforeClass
	public static void setupClass() {
		Event.muteForTests();
		setupBackend();
	}

	@AfterClass
	public static void teardownClass() {
	    teardownBackend();
	}

	private Run testQuotaCreateRun(User user, String cloud)
			throws ValidationException {
		Module deployment = new DeploymentModule("deployment1");
		return new Run(deployment, RunType.Orchestration, new HashSet<String>(Arrays.asList(cloud)), user);
	}

	private User testQuotaCreateUser() throws ValidationException {
		User user = new User(USERNAME);
		user.store();
		return user;
	}

	private void setQuota(User user, String cloud, String value)
			throws ValidationException {
		UserParameter userparam = new UserParameter(cloud
				+ RuntimeParameter.PARAM_WORD_SEPARATOR
				+ QuotaParameter.QUOTA_VM_PARAMETER_NAME, value, "");
		userparam.setCategory(cloud);
		user.setParameter(userparam);
		user.store();
	}

	@Test
	public void validateOk() throws ValidationException,
			ConfigurationException, QuotaException {
		String cloud = "cloud1";
		User user = testQuotaCreateUser();
		setQuota(user, cloud, "10");

		Map<String, CloudUsage> usage = new HashMap<>();

		usage.put(cloud, new CloudUsage(cloud, null, null, 9, 0, 0, 0, 0));

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);

		Quota.validate(user, run1, usage);
	}

	@Test(expected = QuotaException.class)
	public void validateFail() throws ValidationException,
			ConfigurationException, QuotaException {
		String cloud = "cloud1";

		User user = testQuotaCreateUser();
		setQuota(user, cloud, "10");

		Map<String, CloudUsage> usage = new HashMap<>();

		usage.put(cloud, new CloudUsage(cloud, null, null, 9, 0, 0, 0, 0));

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);
		run1.addNodeInstanceName("node2", cloud);

		Quota.validate(user, run1, usage);
	}

	@Test
	public void validateNoUsage() throws ValidationException, ConfigurationException, QuotaException {
		User user = testQuotaCreateUser();

		String cloud = "cloud1";

		// create connector
		CommonTestUtil.createConnector(DummyConnector.CLOUD_SERVICE_NAME, cloud,
				new DummySystemConfigurationParametersFactory(cloud));

		// create credentials
		DummyCloudCredDef credCreate = new DummyCloudCredDef(
				cloud,
				"key",
				"secret",
				"dn");
		CloudCredentialCreateTmpl cloudCredentialCreateTmpl = new CloudCredentialCreateTmpl(credCreate);
		Response resp = SscljProxy.post(SscljProxy.BASE_RESOURCE + "credential",
				USERNAME + " USER", cloudCredentialCreateTmpl);
		assertFalse(resp.toString(), SscljProxy.isError(resp));
		SscljTestServer.refresh();

		setQuota(user, cloud, "10");

		Map<String, CloudUsage> usage = new HashMap<>();

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);
		run1.addNodeInstanceName("node2", cloud);

		Quota.validate(user, run1, usage);
	}

	@Test
	public void validateNoQuotaOk() throws ValidationException, ConfigurationException, QuotaException {
		String cloud = "cloud1";
		User user = testQuotaCreateUser();

		Map<String, CloudUsage> usage = new HashMap<>();

		usage.put(cloud, new CloudUsage(cloud, null, null, 9, 0, 0, 0, 0));

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);
		run1.addNodeInstanceName("node2", cloud);

		Quota.validate(user, run1, usage);
	}

	@Test(expected = QuotaException.class)
	public void validateNoQuotaFail() throws ValidationException, ConfigurationException, QuotaException {
		String cloud = "cloud1";
		User user = testQuotaCreateUser();

		Map<String, CloudUsage> usage = new HashMap<>();

		usage.put(cloud, new CloudUsage(cloud, null, null, 19, 0, 0, 0, 0));

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);
		run1.addNodeInstanceName("node2", cloud);

		Quota.validate(user, run1, usage);
	}

	@Test
	public void getValue() throws ValidationException {
		String cloud = "cloud1";
		User user = new User("user");

		// Default value
		assertThat(Quota.getValue(user, cloud),
				is(QuotaParameter.QUOTA_VM_DEFAULT));

		// Connector value
		ServiceConfiguration cfg = Configuration.getInstance().getParameters();
		ServiceConfigurationParameter cfgParameter = new ServiceConfigurationParameter(cloud
				+ RuntimeParameter.PARAM_WORD_SEPARATOR
				+ QuotaParameter.QUOTA_VM_PARAMETER_NAME, "15");
		cfgParameter.setCategory(cloud);
		cfg.setParameter(cfgParameter);

		assertThat(Quota.getValue(user, cloud), is("15"));

		// User value
		UserParameter parameter = new UserParameter(cloud
				+ RuntimeParameter.PARAM_WORD_SEPARATOR
				+ QuotaParameter.QUOTA_VM_PARAMETER_NAME, "10", "");
		parameter.setCategory(cloud);
		user.setParameter(parameter);

		assertThat(Quota.getValue(user, cloud), is("10"));

		// Empty user parameter value
		parameter.setValue("");
		assertThat(Quota.getValue(user, cloud), is("15"));

                // Null value for user
		parameter.setValue(null);
		assertThat(Quota.getValue(user, cloud), is("15"));

                // Empty connector parameter value
		cfgParameter.setValue("");
		assertThat(Quota.getValue(user, cloud),
				is(QuotaParameter.QUOTA_VM_DEFAULT));

                // Null value for connector parameter
		cfgParameter.setValue(null);
		assertThat(Quota.getValue(user, cloud),
				is(QuotaParameter.QUOTA_VM_DEFAULT));
	}

}
