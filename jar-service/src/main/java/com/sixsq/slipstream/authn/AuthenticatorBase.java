package com.sixsq.slipstream.authn;

import org.restlet.Context;
import org.restlet.data.Cookie;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.security.Authenticator;

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;

public abstract class AuthenticatorBase extends Authenticator {

	public AuthenticatorBase(Context context, boolean optional) {
		super(context, optional);
	}

	protected void setLastOnline(User user) {
		user.setLastOnline();
		user = user.store();
	}

	protected void setLastOnline(Cookie cookie) {

		com.sixsq.slipstream.persistence.User user = null;

		try {
			user = CookieUtils.getCookieUser(cookie);
		} catch (ValidationException e) {
			throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
		}

		if (user == null) {
			throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
		}

		setLastOnline(user);
	}
}
