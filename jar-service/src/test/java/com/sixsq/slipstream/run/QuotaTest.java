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
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.QuotaException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.DeploymentModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.QuotaParameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

public class QuotaTest {

	private Run testQuotaCreateRun(User user, String cloud)
			throws ValidationException {
		Module deployment = new DeploymentModule("deployment1");
		return new Run(deployment, RunType.Orchestration, cloud, user);
	}

	private User testQuotaCreateUser() throws ValidationException {
		User user = new User("user");
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

		Map<String, Integer> usage = new HashMap<String, Integer>();
		usage.put(cloud, 9);

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);

		Quota.validate(user, run1.getCloudServiceUsage(), usage);
	}

	@Test(expected = QuotaException.class)
	public void validateFail() throws ValidationException,
			ConfigurationException, QuotaException {
		String cloud = "cloud1";
		User user = testQuotaCreateUser();
		setQuota(user, cloud, "10");

		Map<String, Integer> usage = new HashMap<String, Integer>();
		usage.put(cloud, 9);

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);
		run1.addNodeInstanceName("node2", cloud);

		Quota.validate(user, run1.getCloudServiceUsage(), usage);
	}

	@Test
	public void validateNoUsage() throws ValidationException,
			ConfigurationException, QuotaException {
		String cloud = "cloud1";
		User user = testQuotaCreateUser();
		setQuota(user, cloud, "10");

		Map<String, Integer> usage = new HashMap<String, Integer>();

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);
		run1.addNodeInstanceName("node2", cloud);

		Quota.validate(user, run1.getCloudServiceUsage(), usage);
	}

	@Test
	public void validateNoQuotaOk() throws ValidationException,
			ConfigurationException, QuotaException {
		String cloud = "cloud1";
		User user = testQuotaCreateUser();

		Map<String, Integer> usage = new HashMap<String, Integer>();
		usage.put(cloud, 9);

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);
		run1.addNodeInstanceName("node2", cloud);

		Quota.validate(user, run1.getCloudServiceUsage(), usage);
	}

	@Test(expected = QuotaException.class)
	public void validateNoQuotaFail() throws ValidationException,
			ConfigurationException, QuotaException {
		String cloud = "cloud1";
		User user = testQuotaCreateUser();

		Map<String, Integer> usage = new HashMap<String, Integer>();
		usage.put(cloud, 19);

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeInstanceName("node1", cloud);
		run1.addNodeInstanceName("node2", cloud);

		Quota.validate(user, run1.getCloudServiceUsage(), usage);
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
