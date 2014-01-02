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

import static com.sixsq.slipstream.messages.MessageUtils.MSG_EMAIL_CONFIRMED_FOR_RESET;
import static com.sixsq.slipstream.messages.MessageUtils.MSG_PASSWORD_RESET;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.persistence.Entity;

import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.messages.MessageUtils;
import com.sixsq.slipstream.persistence.OneShotAction;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.RequestUtil;

@Entity
public class ResetPasswordAction extends OneShotAction {

	@SuppressWarnings("unused")
	private ResetPasswordAction() {
	}

	public ResetPasswordAction(String userResourceUrl) {
		super();

		Form form = new Form();
		form.add("userResourceUrl", userResourceUrl);
		setForm(form);
	}

	private void resetValidated(String baseUrlSlash)
			throws SlipStreamRuntimeException, ConfigurationException {

		Form form = getForm();
		String userResourceUrl = form.getFirst("userResourceUrl").getValue();

		User user = User.load(userResourceUrl, false);
		user.randomizePassword();

		try {
			informUserPasswordChanged(user, baseUrlSlash, userResourceUrl);
		} catch (NoSuchAlgorithmException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			throw new ConfigurationException(e.getMessage());
		}

		user = user.store();
	}

	private void informUserPasswordChanged(User user, String baseUrlSlash,
			String userResourceUrl) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		String username = user.getName();
		String password = user.getPassword();
		String url = baseUrlSlash + userResourceUrl;

		String msg = MessageUtils.format(MSG_PASSWORD_RESET, username,
				password, url);

		// now that the random password has been sent to the user
		// hash it before it is stored
		user.hashAndSetPassword(user.getPassword());

		Notifier.sendNotification(user.getEmail(), msg);
	}

	@Override
	public Representation doAction(Request request)
			throws SlipStreamRuntimeException, ConfigurationException {

		String baseUrlSlash = RequestUtil.getBaseUrlSlash(request);

		resetValidated(baseUrlSlash);

		String msg = MessageUtils.format(MSG_EMAIL_CONFIRMED_FOR_RESET);

		return new StringRepresentation(msg);
	}
}
