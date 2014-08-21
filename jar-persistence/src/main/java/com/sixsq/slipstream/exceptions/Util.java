package com.sixsq.slipstream.exceptions;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class Util {

	public static void throwUnauthorized() {
		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
				"You are not allowed to access this resource");
	}

	public static void throwClientError(Throwable e) {
		throwClientError(e.getMessage());
	}

	public static void throwClientError(String message) {
		throwClientError(Status.CLIENT_ERROR_BAD_REQUEST, message);
	}

	public static void throwClientConflicError(String message) {
		throwClientError(Status.CLIENT_ERROR_CONFLICT, message);
	}

	public static void throwClientConflicError(String message, Throwable e) {
		throwClientError(Status.CLIENT_ERROR_CONFLICT, message, e);
	}

	public static void throwClientForbiddenError() {
		throwClientError(Status.CLIENT_ERROR_FORBIDDEN, "Forbidden");
	}

	public static void throwClientForbiddenError(String message) {
		throwClientError(Status.CLIENT_ERROR_FORBIDDEN, message);
	}

	public static void throwClientForbiddenError(Throwable e) {
		throwClientError(Status.CLIENT_ERROR_FORBIDDEN, e);
	}

	public static void throwClientBadRequest(String message) {
		throwClientError(Status.CLIENT_ERROR_BAD_REQUEST, message);
	}

	public static void throwNotFoundResource() {
		throwClientError(Status.CLIENT_ERROR_NOT_FOUND, "Not found");
	}

	public static void throwClientValidationError(String message) {
		throwClientError(Status.CLIENT_ERROR_BAD_REQUEST, "Validation error: "
				+ message);
	}

	public static void throwClientConflicError(Throwable e) {
		throwClientError(Status.CLIENT_ERROR_CONFLICT, e);
	}

	public static void throwClientError(Status status, String message) {
		throw new ResourceException(status, message);
	}

	public static void throwClientError(Status status, Throwable e) {
		throw new ResourceException(status, e);
	}

	public static void throwClientError(Status status, String message, Throwable e) {
		throw new ResourceException(status, message, e);
	}

	public static void throwServerError(Throwable e) {
		throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
	}

	public static void throwServerError(String message) {
		throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message);
	}

	public static void throwServerError(String message, Throwable e) {
		throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message, e);
	}

	public static void throwConfigurationException(Throwable e) {
		e.printStackTrace();
		throwServerError(e);
	}
}
