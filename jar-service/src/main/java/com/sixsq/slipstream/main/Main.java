package com.sixsq.slipstream.main;

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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.data.Protocol;

import com.sixsq.slipstream.application.RootApplication;
import com.sixsq.slipstream.exceptions.ValidationException;

public class Main {

	private Main() {
	}

	protected static final Logger log = Logger.getLogger(Main.class.toString());

	public static void main(String[] args) throws ValidationException {

		Component component = new Component();

		component.getServers().add(Protocol.HTTP, 8182);
		component.getClients().add(Protocol.FILE);
		component.getClients().add(Protocol.HTTP);
		Application rootApplication = new RootApplication();
		component.getDefaultHost().attach("", rootApplication);

		try {
			component.start();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Start error", e);
			log.severe("Starting SlipStream FAILED!");
			System.exit(1);
		}
		log.info("SlipStream started!");
	}

}
