package com.sixsq.slipstream.module;

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

import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Form;

import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.ResourceTestBase;

public class ImageFormProcesorTest {

	protected static final String PASSWORD = "password";
	protected static User user = ResourceTestBase.createUser("test", PASSWORD);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ResourceTestBase.storeUser(user);
	}

	@Test
	public void newNameIllegal() throws BadlyFormedElementException,
			SlipStreamClientException {
		User user = ResourceTestBase.createUser("test");

		String imageName = "newNameIllegal";
		Module image = new ImageModule(imageName);
		image.store();

		ImageFormProcessor processor = new ImageFormProcessor(user);

		Form form = new Form();

		form.add("name", "new");

		try {
			processor.processForm(form);			
			fail("Illegal name not checked");
		} catch (ValidationException e) {
		}

		image.remove();
	}
}
