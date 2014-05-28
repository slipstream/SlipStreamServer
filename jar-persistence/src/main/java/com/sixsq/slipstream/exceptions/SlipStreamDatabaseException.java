package com.sixsq.slipstream.exceptions;

public class SlipStreamDatabaseException extends SlipStreamInternalException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4188939153575174553L;

	public SlipStreamDatabaseException(String message, Throwable exception) {
		super(message, exception);
	}

	public SlipStreamDatabaseException(Throwable exception) {
		super(exception);
	}

	public SlipStreamDatabaseException(String message) {
		super(message);
	}

	public SlipStreamDatabaseException() {
	}

}
