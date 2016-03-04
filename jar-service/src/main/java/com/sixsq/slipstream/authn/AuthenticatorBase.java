package com.sixsq.slipstream.authn;

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import org.hibernate.StaleObjectStateException;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.data.Cookie;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.security.Authenticator;

import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;

public abstract class AuthenticatorBase extends Authenticator {

	public AuthenticatorBase(Context context, boolean optional) {
		super(context, optional);
	}

	protected void setLastOnline(User user) {
		user.setLastOnline();
		try {
			user = user.store();
		} catch (StaleObjectStateException e) {
		} catch (RollbackException e) {
		} catch (OptimisticLockException e) {
		}
	}

	protected void setLastOnline(Cookie cookie) {

		User user = null;

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

	static public void setUserInRequest(User user, Request request) {
		request.getAttributes().put(User.REQUEST_KEY, user);
	}
}
