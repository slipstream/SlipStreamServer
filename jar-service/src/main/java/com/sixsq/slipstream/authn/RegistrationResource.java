package com.sixsq.slipstream.authn;

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

import static com.sixsq.slipstream.messages.MessageUtils.MSG_CONFIRM_EMAIL;
import static com.sixsq.slipstream.messages.MessageUtils.MSG_ERROR_SENDING_EMAIL;
import static com.sixsq.slipstream.messages.MessageUtils.MSG_REGISTRATION_SENT;

import java.io.IOException;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.action.OneShotAction;
import com.sixsq.slipstream.action.UserEmailValidationAction;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.InvalidElementException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.messages.MessageUtils;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.resource.SimpleRepresentationBaseResource;
import com.sixsq.slipstream.util.Notifier;
import com.sixsq.slipstream.util.ResourceUriUtil;

public class RegistrationResource extends SimpleRepresentationBaseResource {

	@Post("form")
	public void createNewAccount(Representation entity) throws ResourceException {

		checkIsAllowed();

		User newUser = processUser(entity);

		try {
			checkUserDoesntExist(newUser);
		} catch (ConfigurationException e) {
			throwConfigurationException(e);
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		OneShotAction action = createConfirmationEntry(newUser);

		notifyUser(newUser, action);

		newUser.store();

		setPostResponse(MessageUtils.format(MSG_REGISTRATION_SENT), MediaType.TEXT_PLAIN);
	}

	private void checkIsAllowed() {
		boolean allow = true; // allowed by default
		String key = ServiceConfiguration.RequiredParameters.SLIPSTREAM_REGISTRATION_ENABLE.getName();
		Parameter parameter = getConfiguration().getParameters().get(key);
		if (parameter != null) {
			allow = parameter.isTrue();
		}
		if (!allow) {
			throwClientForbiddenError("Self registration disabled");
		}
	}

	private User processUser(Representation entity) {
		User newUser;
		try {
			newUser = processEntityAsForm(entity);
		} catch (SlipStreamException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, e.getMessage());
		}
		return newUser;
	}

	private void notifyUser(User newUser, OneShotAction action) {
		String baseUrlSlash = ResourceUriUtil.getBaseUrlSlash(getRequest());
		String confirmUrl = baseUrlSlash + "action/" + action.getUuid() + "/confirm";

		String msg = MessageUtils.format(MSG_CONFIRM_EMAIL, confirmUrl);

		boolean sendOk = Notifier.sendNotification(getConfiguration(), newUser.getEmail(), msg);

		if (!sendOk) {

			String key = ServiceConfiguration.RequiredParameters.SLIPSTREAM_REGISTRATION_EMAIL.getName();
			Parameter parameter = getConfiguration().getParameter(key);
			String adminEmail = parameter.getValue();

			String errorMsg = MessageUtils.format(MSG_ERROR_SENDING_EMAIL, adminEmail);

			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, errorMsg);
		}
	}

	private OneShotAction createConfirmationEntry(User newUser) {
		OneShotAction action = new UserEmailValidationAction("user/" + newUser.getName());
		action.store();
		return action;
	}

	public void checkUserDoesntExist(User user) throws ConfigurationException, ValidationException {

		if (User.loadByName(user.getName()) != null) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, "Sorry but user " + user.getName()
					+ " already exists. Please choose another.");
		}

	}

	public User processEntityAsForm(Representation entity) throws SlipStreamException {

		User user = null;

		try {

			Form form = new Form(entity.getText());

			String username = form.getFirstValue("username");

			// Create the user object.
			user = new User(username);

			user.setFirstName(form.getFirstValue("firstname"));
			user.setLastName(form.getFirstValue("lastname"));

			user.setOrganization(form.getFirstValue("organization"));

			user.setEmail(form.getFirstValue("email"));

			user.randomizePassword();

			User.validateMinimumInfo(user);

			String agreement = form.getFirstValue("agreement");
			if (agreement == null) {
				throw new InvalidElementException("You must agree to the Terms of Service to register.");
			}

		} catch (IOException e) {

			String msg = "Cannot process information: " + e.getMessage();
			throw new InvalidElementException(msg);
		}

		return user;
	}

	@Override
	protected String getPageRepresentation() {
		return "registration";
	}

	public static String getResourceRoot() {
		return "/register";
	}
}
