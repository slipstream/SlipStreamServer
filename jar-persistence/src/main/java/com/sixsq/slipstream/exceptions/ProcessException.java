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

@SuppressWarnings("serial")
public class ProcessException extends SlipStreamRuntimeException {

	String stdout = "";
	
	public ProcessException(String message, String stdout, Throwable exception) {
		super(message, exception);
		this.stdout = stdout;  
	}
	
	public ProcessException(String message, String stdout) {
		super(message);
		this.stdout = stdout;  
	}
	
	public ProcessException(String message, Throwable exception) {
		super(message, exception);
	}

	public ProcessException(Throwable exception) {
		super(exception);
	}

	public ProcessException(String message) {
		super(message);
	}

	public ProcessException() {
		super();
	}

	public String getStdOut(){
		return stdout;
	}
	
}


