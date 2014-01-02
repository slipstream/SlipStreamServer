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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.util.SerializationUtil;

public class ParameterTest {

	@Test(expected = IllegalArgumentException.class)
	public void nullParameterName() throws SlipStreamClientException {

		new RunParameter(null, "ok", "ok");
	}

	@Test
	public void serializationWorks() {
		RunParameter parameter = new RunParameter("dummy", "ok", "ok");

		String serialization = SerializationUtil.toXmlString(parameter);

		assertNotNull(serialization);
		assertFalse("".equals(serialization));

		Document document = SerializationUtil.toXmlDocument(parameter);

		assertNotNull(document);
	}

	@Test(expected = ValidationException.class)
	public void setInvalidEnumTypeValue() throws ValidationException {
		UserParameter parameter = new UserParameter("", "doesntexist", "desc");

		parameter.setEnumValues(InstanceTypeTest.getValues());

		parameter.setValue("setInvalidEnumTypeValue");
		parameter.validateValue();
	}

	@Test
	public void setValidEnumTypeValue() throws ValidationException {
		UserParameter parameter = new UserParameter("", "", "");

		parameter.setEnumValues(InstanceTypeTest.getValues());

		parameter.setValue(InstanceTypeTest.C1_MEDIUM.getValue());
	}

	@Test
	public void setAnyCategory() throws ValidationException {
		ModuleParameter p = new ModuleParameter("name");
		p.setCategory("something");
		assertThat(p.getCategory(), is("something"));
	}

	@Test(expected = ValidationException.class)
	public void setInvalidStringValue() throws ValidationException {
		ModuleParameter p = new ModuleParameter("name");
		p.setType(ParameterType.RestrictedString);
		p.setValue("'");
		p.validateValue();
	}

	@Test(expected = ValidationException.class)
	public void setInvalidTextValue() throws ValidationException {
		ModuleParameter p = new ModuleParameter("name");
		p.setType(ParameterType.RestrictedText);
		p.setValue("'");
		p.validateValue();
	}

	@Test(expected = ValidationException.class)
	public void setInvalidPassordValue() throws ValidationException {
		ModuleParameter p = new ModuleParameter("name");
		p.setType(ParameterType.Password);
		p.setValue("'");
		p.validateValue();
	}

	@Test
	public void isTrue() throws ValidationException {
		ModuleParameter p;

		p = new ModuleParameter("name", "true", "");
		assertTrue(p.isTrue());

		p = new ModuleParameter("name", "false", "");
		assertFalse(p.isTrue());

		p = new ModuleParameter("name", "", "");
		assertFalse(p.isTrue());

		p = new ModuleParameter("name", null, "");
		assertFalse(p.isTrue());
	}
}
