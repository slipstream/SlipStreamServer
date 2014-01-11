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

import org.junit.Test;

import com.sixsq.slipstream.exceptions.ValidationException;

public class ModuleTest {

	@Test
	public void invalidNames() {
		String[] invalidNames = { null, "", "1", "111", "string with spaces"};

		for (String name : invalidNames) {
			try {
				new ImageModule(name);
				fail("invalid Module name did not throw an exception: " + name);
			} catch (ValidationException e) {
				// OK.
			}
		}
	}
	
	@Test
	public void getParameterValue() throws ValidationException {
		Module module = new ImageModule("aname");
		assertThat(module.getParameterValue("doesnt_exists", "my value"), is("my value"));
	}	
}