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

import static com.sixsq.slipstream.messages.MessageUtils.MSG_EMAIL_CONFIRMED_FOR_RESET;
import static com.sixsq.slipstream.messages.MessageUtils.MSG_PASSWORD_RESET;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import com.sixsq.slipstream.user.UserResource;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.messages.MessageUtils;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.Notifier;
import com.sixsq.slipstream.util.ResourceUriUtil;

public class ResetPasswordActionPerformer extends OneShotActionPerformer {

	public ResetPasswordActionPerformer(OneShotAction action) {
		super(action);
	}

	private void resetValidated(String baseUrlSlash)
			throws SlipStreamRuntimeException, ConfigurationException,
			ValidationException {

		Form form = getForm();
		String userResourceUrl = form.getFirst("userResourceUrl").getValue();

		User user = User.loadNoParams(userResourceUrl);
		user.randomizePassword();

		try {
			informUserPasswordChanged(user, baseUrlSlash, userResourceUrl);
		} catch (NoSuchAlgorithmException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			throw new ConfigurationException(e.getMessage());
		}

		user.store();

		UserResource.postEventPasswordReseted(user);
	}

	private void informUserPasswordChanged(User user, String baseUrlSlash,
			String userResourceUrl) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		String username = user.getName();
		String password = user.getHashedPassword();
		String url = baseUrlSlash + userResourceUrl;

		String msg = MessageUtils.format(MSG_PASSWORD_RESET, username,
				password, url);

		// now that the random password has been sent to the user
		// hash it before it is stored
		user.hashAndSetPassword(user.getHashedPassword());

		Notifier.sendNotification(user.getEmail(), msg);
	}

	@Override
	public Representation doAction(Request request)
			throws SlipStreamRuntimeException, ConfigurationException {

		String baseUrlSlash = ResourceUriUtil.getBaseUrlSlash(request);

		try {
			resetValidated(baseUrlSlash);
		} catch (ValidationException e) {
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, e.getMessage());
		}

		String msg = MessageUtils.format(MSG_EMAIL_CONFIRMED_FOR_RESET);

		return new StringRepresentation(msg);
	}
}
