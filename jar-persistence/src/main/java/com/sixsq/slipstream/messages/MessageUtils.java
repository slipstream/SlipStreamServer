package com.sixsq.slipstream.messages;

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

import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class MessageUtils {

	private static final String bundleName = "com.sixsq.slipstream.messages.Messages";

	private static final ConcurrentHashMap<Locale, ResourceBundle> bundles = new ConcurrentHashMap<Locale, ResourceBundle>();

	private static final Locale defaultLocale = Locale.getDefault();

	public static final String MSG_ACCOUNT_APPROVED = "MSG_ACCOUNT_APPROVED";

	public static final String MSG_ACCOUNT_REJECTED = "MSG_ACCOUNT_REJECTED";

	public static final String MSG_PASSWORD_RESET = "MSG_PASSWORD_RESET";

	public static final String MSG_NEW_USER_NOTIFICATION = "MSG_NEW_USER_NOTIFICATION";

	public static final String MSG_INVALID_ACTION_URL = "MSG_INVALID_ACTION_URL";

	public static final String MSG_REGISTRATION_SENT = "MSG_REGISTRATION_SENT";

	public static final String MSG_RESET_SENT = "MSG_RESET_SENT";

	public static final String MSG_CONFIRM_EMAIL = "MSG_CONFIRM_EMAIL";

	public static final String MSG_CONFIRM_RESET = "MSG_CONFIRM_RESET";

	public static final String MSG_ERROR_SENDING_EMAIL = "MSG_ERROR_SENDING_EMAIL";

	public static final String MSG_EMAIL_CONFIRMED = "MSG_EMAIL_CONFIRMED";

	public static final String MSG_EMAIL_CONFIRMED_FOR_RESET = "MSG_EMAIL_CONFIRMED_FOR_RESET";

	private MessageUtils() {
	}

	private static ResourceBundle getBundle(Locale locale) {
		ResourceBundle bundle = bundles.get(locale);
		if (bundle != null) {
			return bundle;
		} else {
			return createBundle(locale);
		}
	}

	private static ResourceBundle createBundle(Locale locale) {
		try {
			ResourceBundle bundle = ResourceBundle
					.getBundle(bundleName, locale);
			if (bundles.putIfAbsent(locale, bundle) == null) {
				return bundle;
			} else {
				return bundles.get(locale);
			}
		} catch (MissingResourceException mre) {
			throw new SlipStreamRuntimeException(mre.getMessage());
		}

	}

	private static String getMessageTemplate(Locale locale, String key) {
		return getBundle(locale).getString(key);
	}

	public static String format(String key, Object... args) {
		return format(defaultLocale, key, args);
	}

	public static String format(Locale locale, String key, Object... args) {
		return MessageFormat.format(getMessageTemplate(locale, key), args);
	}

}
