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

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.User.State;
import com.sixsq.slipstream.persistence.UserParameter;

/**
 * For tests documentation
 * 
 * @see UserFormProcessorTest
 * 
 */
public class UserFormProcessor extends FormProcessor<User, UserParameter> {

	public UserFormProcessor(User user) {
		super(user);
	}

	public UserFormProcessor(User parametrized, User user) {
		super(user);
		setParametrized(parametrized);
	}

	@Override
	protected void parseForm() throws ValidationException, NotFoundException {

		super.parseForm();

		String name = getForm().getFirstValue("name");

		setParametrized(getOrCreateParameterized(name));
		User user = (User) getParametrized();
		user.setName(name);

		user.setFirstName(getForm().getFirstValue("firstname"));
		user.setLastName(getForm().getFirstValue("lastname"));
		user.setEmail(getForm().getFirstValue("email"));
		user.setOrganization(getForm().getFirstValue("organization"));
		parseState(getForm(), user);

		User dbUser = User.loadByName(name);

		processPassword(getForm(), dbUser);
		processIsSuper(getForm());
	}

	private void parseState(Form form, User user) {
		String state = form.getFirstValue("state");
		if (state != null) {
			user.setState(State.valueOf(state));
		}
	}

	@Override
	protected User getOrCreateParameterized(String name)
			throws ValidationException {
		User user = getParametrized();
		if (user == null) {
			user = new User(name);
		}
		return user;
	}

	@Override
	protected UserParameter createParameter(String name, String value,
			String description) throws SlipStreamClientException {

		UserParameter parameter;

		try {
			parameter = new UserParameter(name, value, description);
		} catch (IllegalArgumentException ex) {
			throw (new SlipStreamClientException(ex.getMessage(), ex));
		}

		parameter.setContainer(getParametrized());

		return parameter;
	}

	private void processPassword(Form form, User dbUser)
			throws ValidationException {

		Passwords passwords = extractPasswords(form);

		boolean changePassword = shouldChangePassword(passwords, dbUser);

		String password;
		if (changePassword) {
			password = passwords.newPassword1;
		} else {
			password = (isNewUser(dbUser) ? null : dbUser.getPassword());
		}
		getParametrized().setPassword(password);
	}

	private Passwords extractPasswords(Form form) {
		String password1 = form.getFirstValue("password1");
		String password2 = form.getFirstValue("password2");
		String oldPassword = form.getFirstValue("oldPassword");
		return new Passwords(oldPassword, password1, password2);
	}

	private boolean shouldChangePassword(Passwords passwords, User dbUser)
			throws ValidationException {

		if (!isSet(passwords.newPassword1) && !isSet(passwords.newPassword2)) {
			return false;
		}

		compareNewPasswords(passwords);

		boolean notNew = !isNewUser(dbUser);
		boolean notSuper = !getUser().isSuper();
		boolean superChangingSuper = false;;
		if (notNew) {
			superChangingSuper = (!notSuper && dbUser.isSuper());
		}

		if (notNew && (notSuper || superChangingSuper)) {
			compareOldAndNewPasswords(passwords.oldPassword,
					dbUser.getPassword());
		}

		return true;
	}

	private boolean isNewUser(User dbUser) {
		return dbUser == null;
	}

	private void compareNewPasswords(Passwords passwords) {
		if (!isSet(passwords.newPassword1) || !isSet(passwords.newPassword2)) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					"Password cannot be changed to an empty value");
		}

		if (!passwords.newPassword1.equals(passwords.newPassword2)) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					"New passwords are not identical");
		}
	}

	private void compareOldAndNewPasswords(String oldPasswordFromUser,
			String oldPasswordFromDb) {

		if (oldPasswordFromUser == null) {
			throwWrongOldPassword();
		}

		if (!oldPasswordFromUser.equals(oldPasswordFromDb)) {
			throwWrongOldPassword();
		}
	}

	private void throwWrongOldPassword() {
		throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
				"Wrong old password");
	}

	private void processIsSuper(Form form) {
		if (getUser().isSuper()) {
			String isSuper = form.getFirstValue("issuper");
			getParametrized().setSuper("on".equals(isSuper));
		}
	}

}
