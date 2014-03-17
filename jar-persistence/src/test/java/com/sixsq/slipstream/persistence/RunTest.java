package com.sixsq.slipstream.persistence;

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
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.LazyInitializationException;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.QuotaException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.statemachine.States;

public class RunTest {

	@Test
	public void loadWithRuntimeParameters() throws ValidationException, NotFoundException, AbortException {
		Module image = new ImageModule();

		Run run = new Run(image, RunType.Run, "test", new User("user"));

		run.assignRuntimeParameter("ss:key", "value", "description");

		run.store();

		run = Run.loadFromUuid(run.getUuid());

		try {
			run.getRuntimeParameterValue("ss:key");
			fail();
		} catch (LazyInitializationException ex) {
		}

		run = Run.loadRunWithRuntimeParameters(run.getUuid());

		assertThat(run.getRuntimeParameterValue("ss:key"), is("value"));
	}

	@Test
	public void oldRuns() throws ValidationException, NotFoundException, AbortException {

		Module image = new ImageModule();

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR, -2);
		Date twoHourBack = calendar.getTime();

		List<Run> before = Run.listOldTransient();

		Run done = new Run(image, RunType.Run, "test", new User("user"));
		done.setStart(twoHourBack);
		done.setState(States.Done);
		done.store();

		Run aborting = new Run(image, RunType.Run, "test", new User("user"));
		aborting.setStart(twoHourBack);
		aborting.setState(States.Aborting);
		aborting.store();

		List<Run> transiant = Run.listOldTransient();

		assertThat(transiant.size(), is(before.size() + 1));

		done.remove();
		aborting.remove();
	}

	public void addNodes() throws ValidationException{


	}

	private Run testQuotaCreateRun(User user, String cloud) throws ValidationException{
		Module deployment = new DeploymentModule("deployment1");
		return new Run(deployment, RunType.Orchestration, cloud, user);
	}

	private User testQuotaCreateUser() throws ValidationException{
		User user = new User("user");
		user.store();
		return user;
	}

	private void setQuota(User user, String cloud, String value) throws ValidationException{
		UserParameter userparam = new UserParameter(
				cloud +
				RuntimeParameter.PARAM_WORD_SEPARATOR +
				Run.QUOTA_VM_PARAMETER_NAME, value, "");
		userparam.setCategory(cloud);
		user.setParameter(userparam);
		user.store();
	}

	@Test
	public void validateQuotaOk() throws ValidationException, ConfigurationException, QuotaException{
		String cloud = "cloud1";
		User user = testQuotaCreateUser();
		setQuota(user, cloud, "10");

		Map<String, Integer> usage = new HashMap<String, Integer>();
		usage.put(cloud, 9);

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeName("node1", cloud);

		run1.validateQuota(usage);
	}

	@Test(expected=QuotaException.class)
	public void validateQuotaFail() throws ValidationException, ConfigurationException, QuotaException{
		String cloud = "cloud1";
		User user = testQuotaCreateUser();
		setQuota(user, cloud, "10");

		Map<String, Integer> usage = new HashMap<String, Integer>();
		usage.put(cloud, 9);

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeName("node1", cloud);
		run1.addNodeName("node2", cloud);

		run1.validateQuota(usage);
	}

	@Test
	public void validateQuotaNoUsage() throws ValidationException, ConfigurationException, QuotaException{
		String cloud = "cloud1";
		User user = testQuotaCreateUser();
		setQuota(user, cloud, "10");

		Map<String, Integer> usage = new HashMap<String, Integer>();

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeName("node1", cloud);
		run1.addNodeName("node2", cloud);

		run1.validateQuota(usage);
	}


	@Test(expected=QuotaException.class)
	public void validateQuotaNoQuota() throws ValidationException, ConfigurationException, QuotaException{
		String cloud = "cloud1";
		User user = testQuotaCreateUser();

		Map<String, Integer> usage = new HashMap<String, Integer>();
		usage.put(cloud, 9);

		Run run1 = testQuotaCreateRun(user, cloud);

		run1.addNodeName("node1", cloud);
		run1.addNodeName("node2", cloud);

		run1.validateQuota(usage);
	}
}

