package com.sixsq.slipstream.user;

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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.sixsq.slipstream.ssclj.app.CIMITestServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.restlet.data.Form;
import org.restlet.data.Parameter;

import com.sixsq.slipstream.exceptions.BadlyFormedElementException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ParameterCategory;
import com.sixsq.slipstream.persistence.ParameterType;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;

public class UserFormProcessorTest {

	private static String prefix = "parameter--";
	private static int count = 0;
	private User user;

	@BeforeClass
	public static void setupClass() throws Exception {
		CIMITestServer.start();
	}

	@AfterClass
	public static void teardownClass() throws Exception {
		CIMITestServer.stop();
	}

	@Before
	public void setup() throws ValidationException {
		user = new User("test");
	}

	@After
	public void tearDown() {
	}

	@Test
	public void processParameters() throws BadlyFormedElementException,
			SlipStreamClientException {

		user = new User("test");

		Form form = createForm();

		UserParameter parameter = new UserParameter("p", "v", "d");
		parameter.setCategory(ParameterCategory.General);
		parameter.setMandatory(true);
		parameter.setType(ParameterType.String);

		fillForm(parameter, form);

		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);

		UserParameter processed = processor.getParametrized().getParameter("p");
		assertThat(processed.getName(), is(parameter.getName()));
		assertThat(processed.getValue(), is(parameter.getValue()));
		assertThat(processed.getDescription(), is(parameter.getDescription()));
		assertThat(processed.getCategory(), is(parameter.getCategory()));
		assertThat(processed.isMandatory(), is(parameter.isMandatory()));
		assertThat(processed.getType(), is(parameter.getType()));
	}

	private Form createForm() {
		Form form = new Form();
		form.set("name", "test", false);
		return form;
	}

	@Test
	public void multiplyDefinedParametersAreIgnored()
			throws BadlyFormedElementException, SlipStreamClientException {

		user = new User("test");

		Form form = createForm();

		UserParameter parameter = new UserParameter("p", "v", "d");
		parameter.setCategory(ParameterCategory.General);
		parameter.setMandatory(true);
		parameter.setType(ParameterType.String);

		fillForm(parameter, form);
		fillForm(parameter, form);

		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);

		assertThat(processor.getParametrized().getParameters().size(), is(1));
	}

	@Test(expected = SlipStreamClientException.class)
	public void missingName() throws BadlyFormedElementException,
			SlipStreamClientException {
		user = new User("test");

		Form form = createForm();

		UserParameter parameter = new UserParameter("p", "v", "d");

		fillForm(parameter, form);
		form.removeAll(prefix + count + "--name");
		form.add(prefix + count + "--name", null);

		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);
	}

	@Ignore
	@Test(expected = SlipStreamClientException.class)
	public void missingValueForInput() throws BadlyFormedElementException,
			SlipStreamClientException {

		ParameterCategory category = ParameterCategory.Input;
		missingValue(category);
	}

	@Ignore
	@Test(expected = SlipStreamClientException.class)
	public void missingValueForOutput() throws BadlyFormedElementException,
			SlipStreamClientException {

		ParameterCategory category = ParameterCategory.Output;
		missingValue(category);
	}

	private void missingValue(ParameterCategory category)
			throws BadlyFormedElementException, SlipStreamClientException {
		user = new User("test");

		Form form = createForm();

		UserParameter parameter = new UserParameter("p", null, "d");
		parameter.setCategory(category);

		fillForm(parameter, form);

		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidType() throws BadlyFormedElementException,
			SlipStreamClientException {

		user = new User("test");

		Form form = createForm();

		UserParameter parameter = new UserParameter("p", "v", "d");
		parameter.setCategory(ParameterCategory.General);
		parameter.setMandatory(true);

		fillForm(parameter, form);

		count --;
		Parameter invalid = createParameterType("invalid");
		count ++;

		form.removeAll(invalid.getName());
		form.add(invalid);


		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);
	}

	@Test
	public void passwordSetFirstTimeWhenNull()
			throws BadlyFormedElementException, SlipStreamClientException {

		Form form = createForm();

		UserParameter emptyPassword = createPasswordParameter();
		emptyPassword.setValue(null);
		fillForm(emptyPassword, form);

		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);

		assertNull(processor.getParametrized().getParameter("password")
				.getValue());
	}

	@Test
	public void passwordSetFirstTimeWhenEmpty()
			throws BadlyFormedElementException, SlipStreamClientException {

		Form form = createForm();

		UserParameter emptyPassword = createPasswordParameter();
		emptyPassword.setValue("");
		fillForm(emptyPassword, form);

		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);

		assertThat(processor.getParametrized().getParameter("password")
				.getValue(), is(""));
	}

	@Test
	public void passwordNotOverwrittenWhenEmpty()
			throws BadlyFormedElementException, SlipStreamClientException {

		user = new User("test");

		UserParameter password = createPasswordParameter();
		user.setParameter(password);

		Form form = createForm();

		UserParameter emptyPassword = createPasswordParameter();
		emptyPassword.setValue("");
		fillForm(emptyPassword, form);

		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);

		assertThat(user.getParameter("password").getValue(),
				is(password.getValue()));
	}

	@Test
	public void passwordSetWhenNormalString()
			throws BadlyFormedElementException, SlipStreamClientException {

		user = new User("test");

		UserParameter password = createPasswordParameter();
		user.setParameter(password);

		Form form = createForm();

		UserParameter validPassword = createPasswordParameter();
		password.setValue("1234");
		validPassword.setValue(null);
		fillForm(validPassword, form);

		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);

		assertThat(user.getParameter("password").getValue(), is("1234"));
	}

	private UserParameter createPasswordParameter() throws ValidationException {
		UserParameter password = new UserParameter("password",
				"default_password", "description");
		password.setCategory(ParameterCategory.General);
		password.setMandatory(true);
		password.setType(ParameterType.Password);
		return password;
	}

	@Test
	public void normalPassword() throws BadlyFormedElementException,
			SlipStreamClientException {

		user = new User("test");

		UserParameter password = createPasswordParameter();
		user.setParameter(password);

		Form form = createForm();

		UserParameter emptyPassword = createPasswordParameter();
		emptyPassword.setValue(null);
		fillForm(emptyPassword, form);

		UserFormProcessor processor = new UserFormProcessor(user);
		processor.processForm(form);

		assertThat(user.getParameter("password").getValue(),
				is("default_password"));
	}

	public static void fillForm(UserParameter parameter, Form form) {
		form.add(createParameterName(parameter.getName()));
		form.add(createParameterValue(parameter.getValue()));
		form.add(createParameterDescription(parameter.getDescription()));
		form.add(createParameterCategory(parameter.getCategory()));
		form.add(createParameterMandatory(parameter.isMandatory()));
		form.add(createParameterType(parameter.getType()));
		count ++;
	}

	@Test
	public void randomPassword() throws ValidationException {
		user = new User("randomPasswordUser");
		user.randomizePassword();
		assertThat(user.getHashedPassword(), is(not("0")));

	}

	private static Parameter createParameterName(String name) {
		return createParameter(prefix + count + "--name", name);
	}

	private static Parameter createParameterValue(String value) {
		return createParameter(prefix + count + "--value", value);
	}

	private static Parameter createParameterDescription(String description) {
		return createParameter(prefix + count + "--description", description);
	}

	private static Parameter createParameterCategory(String category) {
		return createParameter(prefix + count + "--category",
				category.toString());
	}

	private static Parameter createParameterMandatory(boolean isMandatory) {
		return createParameter(prefix + count + "--mandatory",
				String.valueOf(isMandatory));
	}

	private static Parameter createParameterType(ParameterType type) {
		return createParameterType(type.toString());
	}

	private static Parameter createParameterType(String type) {
		return createParameter(prefix + count + "--type", type);
	}

	private static Parameter createParameter(String name, String value) {
		return new Parameter(name, value);
	}

}
