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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;

public class ServiceConfigurationTest {

	@Test(expected = IllegalArgumentException.class)
	public void emptyConfigurationInvalid() {
		ServiceConfiguration cfg = new ServiceConfiguration();
		cfg.validate();
	}

	@Test
	public void idIncreasesWithTime() throws InterruptedException {
		ServiceConfiguration first = new ServiceConfiguration();
		Thread.sleep(1000);
		ServiceConfiguration second = new ServiceConfiguration();
		
		String[] parts;
		parts = first.getId().split("/");
		Long firstMilli = Long.valueOf(parts[parts.length-1]);
		parts = second.getId().split("/");
		Long secondMilli = Long.valueOf(parts[parts.length-1]);
		
		assertTrue(secondMilli > firstMilli);
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
	public void validSupportUrl() {
		String value = "http://support.example.org/";
		ServiceConfiguration.RequiredParameters.SLIPSTREAM_SUPPORT_URL
				.validate(value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidSupportUrl() {
		String value = ":invalid-support-url";
		ServiceConfiguration.RequiredParameters.SLIPSTREAM_SUPPORT_URL
				.validate(value);
	}

	@Test
	public void checkInvalidPorts() {
		String[] values = { "-1", "aaa", "65536" };
		for (String value : values) {
			try {
				ServiceConfiguration.RequiredParameters.SLIPSTREAM_MAIL_PORT.validate(value);
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
			ServiceConfiguration.RequiredParameters.SLIPSTREAM_MAIL_PORT.validate(value);
		}
	}

	@Test
	public void ensureMinimalConfigurationIsValid()
			throws ConfigurationException {
		ServiceConfiguration cfg = minimalValidConfiguration();
		cfg.validate();
	}

	@Test
	public void storeLoadCycle() throws InterruptedException,
			ConfigurationException {

		ServiceConfiguration cfg = minimalValidConfiguration();
		cfg.store();
		String first = cfg.getId();

		Thread.sleep(100);

		ServiceConfiguration recoveredCfg = ServiceConfiguration.load();
		assertNotNull(recoveredCfg);
		assertEquals(first, recoveredCfg.getId());

		cfg = minimalValidConfiguration();
		ServiceConfigurationParameter parameter = new ServiceConfigurationParameter(
				ServiceConfiguration.RequiredParameters.SLIPSTREAM_MAIL_USERNAME.getValue(),
				"OK", "");
		cfg.setParameter(parameter);
		cfg.store();
		String second = cfg.getId();

		Thread.sleep(1000);
		recoveredCfg = ServiceConfiguration.load();
		assertNotNull(recoveredCfg);
		assertEquals(second, recoveredCfg.getId());

		ServiceConfigurationParameter recoveredParameter = cfg
				.getParameter(ServiceConfiguration.RequiredParameters.SLIPSTREAM_MAIL_USERNAME
						.getValue());
		assertNotNull(recoveredParameter);
		assertEquals("OK", recoveredParameter.getValue());
	}

	public ServiceConfiguration minimalValidConfiguration()
			throws ConfigurationException {

		ServiceConfiguration cfg = Configuration.getInstance().getParameters();

		// ServiceConfigurationParameter parameter;
		//
		// parameter = new ServiceConfigurationParameter(
		// ServiceConfiguration.AllowedParameter.REGISTRATION_EMAIL,
		// "admin@example.org");
		// cfg.setParameter(parameter);
		//
		// parameter = new ServiceConfigurationParameter(
		// ServiceConfiguration.AllowedParameter.MAIL_HOST,
		// "mail.example.org");
		// cfg.setParameter(parameter);
		//
		// parameter = new ServiceConfigurationParameter(
		// ServiceConfiguration.AllowedParameter.MAIL_USERNAME,
		// "mail.sender");
		// cfg.setParameter(parameter);
		//
		// parameter = new ServiceConfigurationParameter(
		// ServiceConfiguration.AllowedParameter.SLIPSTREAM_SUPPORT_EMAIL,
		// "support@example.org");
		// cfg.setParameter(parameter);
		//
		// parameter = new ServiceConfigurationParameter(
		// ServiceConfiguration.AllowedParameter.SLIPSTREAM_SUPPORT_URL,
		// "http://support.example.org/");
		// cfg.setParameter(parameter);
		//
		// parameter = new ServiceConfigurationParameter(
		// ServiceConfiguration.AllowedParameter.SLIPSTREAM_BASE_URL,
		// "http://support.example.org/");
		// cfg.setParameter(parameter);
		//
		// parameter = new ServiceConfigurationParameter(
		// ServiceConfiguration.AllowedParameter.SLIPSTREAM_BASE_URL,
		// "http://support.example.org/");
		// cfg.setParameter(parameter);

		return cfg;
	}

}
