package com.sixsq.slipstream.action;

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

import static com.sixsq.slipstream.messages.MessageUtils.MSG_ACCOUNT_APPROVED;
import static com.sixsq.slipstream.messages.MessageUtils.MSG_EMAIL_CONFIRMED;
import static com.sixsq.slipstream.messages.MessageUtils.MSG_NEW_USER_NOTIFICATION;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.messages.MessageUtils;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.Notifier;
import com.sixsq.slipstream.util.ResourceUriUtil;

public class UserEmailValidationActionPerformer extends OneShotActionPerformer {

	public UserEmailValidationActionPerformer(OneShotAction action) {
		super(action);
	}

	private void emailValidated(String baseUrlSlash)
			throws ConfigurationException, ValidationException {

		Form form = getForm();
		String userResourceUrl = form.getFirst("userResourceUrl").getValue();

		User user = User.load(userResourceUrl);

		if(user.getState() != User.State.NEW) {
			throw new ValidationException("User not new. Did you already validate this user?");
		}

		user.setState(User.State.ACTIVE);

		try {
			informUserAccountActivated(user, baseUrlSlash, userResourceUrl);
		} catch (NoSuchAlgorithmException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			throw new ConfigurationException(e.getMessage());
		}

		user.store();

		informAdministratorOfUserCreation(baseUrlSlash, userResourceUrl);
	}

	private void informUserAccountActivated(User user, String baseUrlSlash,
			String userResourceUrl) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		String username = user.getName();
		String password = user.getHashedPassword();
		String url = baseUrlSlash + userResourceUrl;

		String msg = MessageUtils.format(MSG_ACCOUNT_APPROVED, username,
				password, url);

		// now that the random password has been sent to the user
		// hash it before it is stored
		user.hashAndSetPassword(user.getHashedPassword());

		Notifier.sendNotification(user.getEmail(), msg);
	}

	private void informAdministratorOfUserCreation(String baseUrlSlash,
			String userResourceUrl) throws ValidationException,
			SlipStreamRuntimeException {

		String msg = MessageUtils.format(MSG_NEW_USER_NOTIFICATION,
				baseUrlSlash + userResourceUrl);

		Notifier.sendNotification(getRegistrationEmail(), msg);
	}

	private String getRegistrationEmail() throws ConfigurationException,
			ValidationException {
		ServiceConfigurationParameter p = Configuration
				.getInstance()
				.getParameters()
				.getParameter(
						ServiceConfiguration.RequiredParameters.SLIPSTREAM_REGISTRATION_EMAIL
								.getName());
		return p.getValue();
	}

	@Override
	public Representation doAction(Request request)
			throws SlipStreamRuntimeException, ConfigurationException {

		String baseUrlSlash = ResourceUriUtil.getBaseUrlSlash(request);

		try {
			emailValidated(baseUrlSlash);
		} catch (ValidationException e) {
			Util.throwClientValidationError(e.getMessage());
		}

		String msg = MessageUtils.format(MSG_EMAIL_CONFIRMED);

		return new StringRepresentation(msg);
	}
}
