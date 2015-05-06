package com.sixsq.slipstream.cookie;

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

import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.CookieSetting;
import org.restlet.data.Form;
import org.restlet.security.Verifier;
import org.restlet.util.Series;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;

/**
 * Contains utilities for handling authentication cookies.
 *
 * @author loomis
 *
 */
public class CookieUtils {

	// For testing, the age defaults to 30 minutes. The value is given in
	// seconds!
	private static final int COOKIE_DEFAULT_AGE = 60 * 60 * 24 * 7; // 7 days

	// Name used to identify the authentication cookie.
	public static String COOKIE_NAME = "com.sixsq.slipstream.cookie";
	private static String COOKIE_PATH = "/";

	// Names of fields containing cookie information.
	public static final String COOKIE_RUN_ID = "com.sixsq.runId";
	public static final String COOKIE_IS_MACHINE = "com.sixsq.isMachine";
	public static final String COOKIE_EXPIRY_DATE = "com.sixsq.expirydate";
	private static final String COOKIE_IDTYPE = "com.sixsq.idtype";
	private static final String COOKIE_IDENTIFIER = "com.sixsq.identifier";
	private static final String COOKIE_SIGNATURE = "com.sixsq.signature";
	private static final String COOKIE_DEFAULT_IDTYPE = "local";


	private static final Set<String> requiredCookieKeys = new TreeSet<String>();
	static {
		requiredCookieKeys.add(COOKIE_IDTYPE);
		requiredCookieKeys.add(COOKIE_IDENTIFIER);
		requiredCookieKeys.add(COOKIE_EXPIRY_DATE);
		requiredCookieKeys.add(COOKIE_SIGNATURE);
	}

	/**
	 * Insert a new authentication cookie into a Response using default id type.
	 *
	 * @param response
	 * @param identifier
	 */
	public static void addAuthnCookie(Response response, String identifier) {
		addAuthnCookie(response, COOKIE_DEFAULT_IDTYPE, identifier);
	}

	/**
	 * Insert a new authentication cookie into a Response using the given
	 * values. None of the arguments can be null.
	 *
	 * @param response
	 * @param idType
	 * @param identifier
	 */
	public static void addAuthnCookie(Response response, String idType,
			String identifier) {

		Series<CookieSetting> cookieSettings = response.getCookieSettings();
		cookieSettings.removeAll(COOKIE_NAME);
		CookieSetting cookieSetting = createAuthnCookieSetting(idType,
				identifier);
		cookieSettings.add(cookieSetting);
	}

	/**
	 * Insert a new authentication cookie into a Request using the given values.
	 * None of the arguments can be null.
	 *
	 * @param identifier
	 * @param response
	 */
	public static void addAuthnCookie(Request request, String identifier) {

		request.getCookies().clear();
		CookieSetting cookieSetting = createAuthnCookieSetting(
				COOKIE_DEFAULT_IDTYPE, identifier);
		request.getCookies().add(cookieSetting);

	}

	/**
	 * Insert a new authentication cookie into a Request using the given values.
	 * None of the arguments can be null.
	 *
	 * @param request
	 * @param identifier
	 * @param cloudServiceName
	 */
	public static void addAuthnCookie(Request request, String identifier,
			String cloudServiceName) {

		request.getCookies().clear();
		CookieSetting cookieSetting = createAuthnCookieSetting(
				COOKIE_DEFAULT_IDTYPE, identifier,
				generateCloudServiceNameProperties(cloudServiceName));
		request.getCookies().add(cookieSetting);
	}

	/**
	 * Creates a new authentication cookie using the provided information. None
	 * of the arguments may be null.
	 *
	 * @param request
	 * @param idType
	 * @param identifier
	 *
	 * @return new authentication cookie
	 */
	private static CookieSetting createAuthnCookieSetting(String idType,
			String identifier) {

		return createAuthnCookieSetting(idType, identifier, new Properties());
	}

	/**
	 * Creates a new authentication cookie using the provided information. None
	 * of the arguments may be null.
	 *
	 * @param idType
	 * @param identifier
	 * @param properties
	 *
	 * @return new authentication cookie
	 */
	private static CookieSetting createAuthnCookieSetting(String idType,
			String identifier, Properties properties) {

		String finalQuery = createCookieValue(idType, identifier, properties);

		CookieSetting cookieSetting = new CookieSetting(COOKIE_NAME, finalQuery);

		cookieSetting.setPath("/");

		cookieSetting.setDomain("");

		cookieSetting.setMaxAge(COOKIE_DEFAULT_AGE);

		return cookieSetting;
	}

	public static String createCookie(String username, String cloudServiceName) {
		return createCookie(username, cloudServiceName, null);
	}

	public static String createCookie(String username, String cloudServiceName,
			Properties extraProperties) {
		Properties properties = generateCloudServiceNameProperties(cloudServiceName);
		if (extraProperties != null) {
			properties.putAll(extraProperties);
		}
		return getCookieName() + "="
				+ CookieUtils.createCookieValue("local", username, properties)
				+ "; Path:/";
	}

	private static Properties generateCloudServiceNameProperties(
			String cloudServiceName) {
		Properties properties = new Properties();
		properties.put(RuntimeParameter.CLOUD_SERVICE_NAME, cloudServiceName);
		return properties;
	}

	public static String createCookieValue(String idType, String identifier,
			Properties properties) {
		// Create the expiration date for the cookie. This is added to be sure
		// that the server has control over this even if a malicious client
		// extends the date of a cookie.
		long expiryMillis = (new Date()).getTime() + COOKIE_DEFAULT_AGE * 1000L;
		String expiryDate = Long.toString(expiryMillis);

		// Create a form (and query) that contains the authentication
		// information to be signed.
		Form form = new Form();
		form.add(COOKIE_IDTYPE, idType);
		form.add(COOKIE_IDENTIFIER, identifier);
		if (!properties.containsKey(COOKIE_EXPIRY_DATE)) {
			form.add(COOKIE_EXPIRY_DATE, expiryDate);
		}

		// Add all parameters
		for (Entry<Object, Object> entry : properties.entrySet()) {
			form.add((String) entry.getKey(), (String) entry.getValue());
		}

		// Create the signed form of the query (i.e. without the signature
		// added).
		String signedQuery = form.getQueryString();
		String signature = CryptoUtils.sign(signedQuery);

		// Add the signature to the form and create final query string.
		form.add(COOKIE_SIGNATURE, signature);
		String finalQuery = form.getQueryString();
		return finalQuery;
	}

	/**
	 * Force the authentication cookie to be deleted from the client's cache.
	 * This inserts an invalid cookie into the Response that expires immediately
	 * (max. age = 0).
	 *
	 * @param response
	 */
	public static void removeAuthnCookie(Response response) {

		// Get the current cookie settings, remove any with the
		// authorization cookie.
		Series<CookieSetting> cookieSettings = response.getCookieSettings();
		cookieSettings.removeAll(COOKIE_NAME);

		// Create cookie to remove any cookies on client side. Add this to the
		// response.
		CookieSetting cookieSetting = createClearingCookie(response);
		cookieSettings.add(cookieSetting);
	}

	/**
	 * Convenience method to create a CookieSetting object from a Response.
	 *
	 * @param response
	 *
	 * @return CookieSetting object that will remove authentication cookie from
	 *         client's cache
	 */
	private static CookieSetting createClearingCookie(Response response) {
		return createClearingCookie(response.getRequest());
	}

	/**
	 * Creates a CookieSetting object that will clear an authentication cookie
	 * from the client's cache.
	 *
	 * @param request
	 * @return CookieSetting object to remove authentication cookie
	 */
	private static CookieSetting createClearingCookie(Request request) {

		CookieSetting cookieSetting = new CookieSetting(COOKIE_NAME, "INVALID");

		cookieSetting.setPath(COOKIE_PATH);

		// Note that the cookie setting/clearing doesn't work correctly if the
		// domain is set.
		cookieSetting.setDomain("");

		// Setting the age to zero forces the cookie to be discarded
		// immediately. The value -1 will discard the cookie at the end of
		// the session.
		cookieSetting.setMaxAge(0);

		return cookieSetting;
	}

	/**
	 * Convenience method to extract an authentication cookie from a request.
	 *
	 * @param request
	 *
	 * @return authentication cookie if the request has one, null otherwise
	 */
	public static Cookie extractAuthnCookie(Request request) {
		return request.getCookies().getFirst(COOKIE_NAME);
	}

	/**
	 * Verify that the given authentication cookie is valid. This checks that
	 * the name is correct, information is complete, the signature is correct,
	 * and that the cookie has not yet expired.
	 *
	 * @param cookie
	 *
	 * @return status code indicating whether the cookie is valid
	 */
	public static int verifyAuthnCookie(Cookie cookie) {

		if (cookie == null || !COOKIE_NAME.equals(cookie.getName())) {
			return Verifier.RESULT_MISSING;
		}

		if (!COOKIE_NAME.equals(cookie.getName())) {
			return Verifier.RESULT_INVALID;
		}

		Form cookieInfo = extractCookieValueAsForm(cookie);

		if (!cookieInfo.getNames().containsAll(requiredCookieKeys)) {
			return Verifier.RESULT_INVALID;
		}

		// Pull out the values from the form.
		String signature = cookieInfo.getFirstValue(COOKIE_SIGNATURE);
		String expiryString = cookieInfo.getFirstValue(COOKIE_EXPIRY_DATE);

		// Recreate a query string without the signature.
		cookieInfo.removeAll(COOKIE_SIGNATURE);
		String signedQuery = cookieInfo.getQueryString();

		// Ensure that the cryptographic signature is correct.
		if (!CryptoUtils.verify(signature, signedQuery)) {
			return Verifier.RESULT_INVALID;
		}

		// Create the expiration date.
		Date expiryDate = dateFromExpiryString(expiryString);

		if (!CookieUtils.isMachine(cookie)) {
			// Check that the cookie has not yet expired.
			if (expiryDate.before(new Date())) {
				return Verifier.RESULT_STALE;
			} else {
				return Verifier.RESULT_VALID;
			}
		}
		return Verifier.RESULT_VALID;

	}

	public static String getCookieUsername(Cookie cookie) {

		String username = null;

		if (cookie != null) {
			Form form = new Form(cookie.getValue());
			username = form.getFirstValue(COOKIE_IDENTIFIER);
		}
		return username;
	}

	public static String getCookieCloudServiceName(Cookie cookie) {

		String cloudServiceName = null;

		if (cookie != null) {
			Form form = new Form(cookie.getValue());
			cloudServiceName = form
					.getFirstValue(RuntimeParameter.CLOUD_SERVICE_NAME);
		}
		return cloudServiceName;
	}

	public static User getCookieUser(Cookie cookie)
			throws ConfigurationException, ValidationException {

		if (cookie != null) {
			Form form = new Form(cookie.getValue());
			String username = form.getFirstValue(COOKIE_IDENTIFIER);
			return User.loadByName(username);
		}

		return null;
	}

	private static Date dateFromExpiryString(String expiryString) {
		try {
			return new Date(Long.parseLong(expiryString));
		} catch (NumberFormatException e) {
			return new Date(0L);
		}
	}

	private static Form extractCookieValueAsForm(Cookie cookie) {
		String value = (cookie != null) ? cookie.getValue() : null;
		return new Form((value != null) ? value : "");
	}

	public static String cookieToString(Cookie cookie) {

		StringBuilder sb = new StringBuilder();

		if (cookie != null) {

			Form cookieInfo = extractCookieValueAsForm(cookie);

			// Pull out the values from the form.
			String signature = cookieInfo.getFirstValue(COOKIE_SIGNATURE);
			String expiryString = cookieInfo.getFirstValue(COOKIE_EXPIRY_DATE);

			// Recreate a query string without the signature.
			cookieInfo.removeAll(COOKIE_SIGNATURE);
			String signedQuery = cookieInfo.getQueryString();

			// Create the expiration date.
			Date expiryDate = dateFromExpiryString(expiryString);

			sb.append("COOKIE: " + cookie.getName() + "\n");
			sb.append("DOMAIN: " + cookie.getDomain() + "\n");
			sb.append("PATH: " + cookie.getDomain() + "\n");
			sb.append("SIGNATURE: " + signature + "\n");
			sb.append("VALIDATED: "
					+ CryptoUtils.verify(signature, signedQuery) + "\n");
			sb.append("EXPIRY STRING: " + expiryString + "\n");
			sb.append("EXPIRY DATE: " + expiryDate + "\n");
			sb.append("CURRENT: " + !expiryDate.before(new Date()) + "\n");

		} else {

			sb.append("COOKIE: NULL\n");

		}

		return sb.toString();

	}

	public static String getCookieName() {
		return COOKIE_NAME;
	}

	public static boolean isMachine(Cookie cookie) {
		Form f = extractCookieValueAsForm(cookie);
		return "true".equals(f.getFirstValue(COOKIE_IS_MACHINE, "false"));
	}

	public static String getRunId(Cookie cookie) {
		Form f = extractCookieValueAsForm(cookie);
		return f.getFirstValue(COOKIE_RUN_ID, null);
	}

}
