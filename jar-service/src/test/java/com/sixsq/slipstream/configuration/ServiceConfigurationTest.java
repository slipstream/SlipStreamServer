package com.sixsq.slipstream.configuration;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.BeforeClass;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;

import com.sixsq.slipstream.es.CljElasticsearchHelper;

public class ServiceConfigurationTest {

	@BeforeClass
	public static void setupClass(){
		CljElasticsearchHelper.createAndInitTestDb();
	}

	@AfterClass
	public static void teardownClass(){
		CljElasticsearchHelper.stopAndUnbindTestDb();
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyConfigurationInvalid() {
		ServiceConfiguration cfg = new ServiceConfiguration();
		cfg.validate();
	}

	@Test
	public void idIsConstant() throws InterruptedException {
		ServiceConfiguration first = new ServiceConfiguration();
		Thread.sleep(1000);
		ServiceConfiguration second = new ServiceConfiguration();

		assertTrue(first.getId().equals(second.getId()));
	}

	@Test
	public void validEmails() {
		String value = "all.ok@example.org";
		ServiceConfiguration.RequiredParameters.SLIPSTREAM_REGISTRATION_EMAIL
				.validate(value);
		ServiceConfiguration.RequiredParameters.SLIPSTREAM_SUPPORT_EMAIL
				.validate(value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidRegistrationEmail() {
		String value = "invalid@email@address.example.org";
		ServiceConfiguration.RequiredParameters.SLIPSTREAM_REGISTRATION_EMAIL
				.validate(value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidSupportEmail() {
		String value = "invalid@email@address.example.org";
		ServiceConfiguration.RequiredParameters.SLIPSTREAM_SUPPORT_EMAIL
				.validate(value);
	}

	@Test
	public void checkInvalidPorts() {
		String[] values = { "-1", "aaa", "65536" };
		for (String value : values) {
			try {
				ServiceConfiguration.RequiredParameters.SLIPSTREAM_MAIL_PORT
						.validate(value);
				fail("illegal value did not throw exception: " + value);
			} catch (IllegalArgumentException e) {
				// OK
			}
		}
	}

	@Test
	public void checkValidPorts() {
		String[] values = { "1", "1010", "65535" };
		for (String value : values) {
			ServiceConfiguration.RequiredParameters.SLIPSTREAM_MAIL_PORT
					.validate(value);
		}
	}

	@Test
	public void ensureMinimalConfigurationIsValid()
			throws ConfigurationException, ValidationException {
		ServiceConfiguration cfg = minimalValidConfiguration();
		cfg.validate();
	}

	@Test
	public void storeLoadCycle() throws InterruptedException,
			ConfigurationException, ValidationException {

		ServiceConfiguration cfg = minimalValidConfiguration();
		cfg.store();
		String first = cfg.getId();

		Thread.sleep(100);

		ServiceConfiguration recoveredCfg = ServiceConfiguration.load();
		assertNotNull(recoveredCfg);
		assertEquals(first, recoveredCfg.getId());

		cfg = minimalValidConfiguration();
		ServiceConfigurationParameter parameter = new ServiceConfigurationParameter(
				ServiceConfiguration.RequiredParameters.SLIPSTREAM_MAIL_USERNAME
						.getName(), "OK", "");
		cfg.setParameter(parameter);
		cfg.store();
		String second = cfg.getId();

		Thread.sleep(1000);
		recoveredCfg = ServiceConfiguration.load();
		assertNotNull(recoveredCfg);
		assertEquals(second, recoveredCfg.getId());

		ServiceConfigurationParameter recoveredParameter = cfg
				.getParameter(ServiceConfiguration.RequiredParameters.SLIPSTREAM_MAIL_USERNAME
						.getName());
		assertNotNull(recoveredParameter);
		assertEquals("OK", recoveredParameter.getValue());
	}

	public ServiceConfiguration minimalValidConfiguration()
			throws ConfigurationException, ValidationException {

		ServiceConfiguration cfg = Configuration.getInstance().getParameters();

		return cfg;
	}

}
