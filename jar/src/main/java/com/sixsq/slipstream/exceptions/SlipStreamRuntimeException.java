package com.sixsq.slipstream.exceptions;

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

/**
 * Marker class to group all of the SlipStream runtime exceptions.
 * 
 * @author loomis
 * 
 */
@SuppressWarnings("serial")
public class SlipStreamRuntimeException extends RuntimeException {

	public SlipStreamRuntimeException(String message, Throwable exception) {
		super(message, exception);
	}

	public SlipStreamRuntimeException(Throwable exception) {
		super(exception);
	}

	public SlipStreamRuntimeException(String message) {
		super(message);
	}

	public SlipStreamRuntimeException() {
		super();
	}

}
