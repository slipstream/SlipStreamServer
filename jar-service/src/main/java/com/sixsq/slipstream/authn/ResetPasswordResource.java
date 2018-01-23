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

import static com.sixsq.slipstream.messages.MessageUtils.MSG_CONFIRM_RESET;
import static com.sixsq.slipstream.messages.MessageUtils.MSG_ERROR_SENDING_EMAIL;
import static com.sixsq.slipstream.messages.MessageUtils.MSG_RESET_SENT;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.action.OneShotAction;
import com.sixsq.slipstream.action.ResetPasswordAction;
import com.sixsq.slipstream.messages.MessageUtils;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.resource.SimpleRepresentationBaseResource;
import com.sixsq.slipstream.util.Notifier;
import com.sixsq.slipstream.util.ResourceUriUtil;

public class ResetPasswordResource extends SimpleRepresentationBaseResource {

	@Post("form")
	public void resetPassword(Representation entity) throws ResourceException {

		User user = null;
		try {
			user = retrieveNamedUserNoParams(entity);
		} catch (ConfigurationException e) {
			throwServerError(e.getMessage());
		} catch (ValidationException e) {
			throwClientValidationError(e.getMessage());
		}

		OneShotAction action = createResetPasswordEntry(user);

		notifyUser(user, action);

		setPostResponse(MessageUtils.format(MSG_RESET_SENT),
				MediaType.TEXT_PLAIN);
	}

	private void notifyUser(User user, OneShotAction action) {
		if (user != null && action != null) {
			String baseUrlSlash = ResourceUriUtil.getBaseUrlSlash(getRequest());
			String confirmUrl = baseUrlSlash + "action/" + action.getUuid()
					+ "/confirm";

			String msg = MessageUtils.format(MSG_CONFIRM_RESET, confirmUrl);

			boolean sendOk = Notifier.sendNotification(getConfiguration(),
					user.getEmail(), msg);

			if (!sendOk) {

				String key = ServiceConfiguration.RequiredParameters.SLIPSTREAM_REGISTRATION_EMAIL
						.getName();
				ServiceConfigurationParameter parameter = getConfiguration()
						.getParameter(key);
				String adminEmail = parameter.getValue();

				String errorMsg = MessageUtils.format(MSG_ERROR_SENDING_EMAIL,
						adminEmail);

				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						errorMsg);
			}
		}
	}

	private OneShotAction createResetPasswordEntry(User user) {
		OneShotAction action = null;
		if (user != null) {
			action = new ResetPasswordAction("user/" + user.getName());
			action.store();
		}
		return action;
	}

	private User retrieveNamedUserNoParams(Representation entity)
			throws ResourceException, ConfigurationException,
			ValidationException {

		Form form = new Form(entity);
		String username = form.getFirstValue("username");

		return User.loadByNameNoParams(username);
	}

	@Override
	protected String getPageRepresentation() {
		return "reset password";
	}

	public static String getResourceRoot() {
		return "/reset";
	}
}
