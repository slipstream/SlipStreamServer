package com.sixsq.slipstream.connector;

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

import java.util.HashSet;
import java.util.logging.Logger;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;

public class Launcher {

	private static Logger logger = Logger.getLogger(Launcher.class.getName());

	public static Run launch(Run run, User user) throws SlipStreamException {

		try {
			run = storeRunKeepModule(run);
			SyncLauncher sl = (new Launcher()).new SyncLauncher(run, user);
			sl.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			run = Run.abort(ex.getMessage(), run.getUuid());
			run = run.store();
		}
		return run;
	}

	private static Run storeRunKeepModule(Run run) throws ValidationException {
		run = Run.loadFromUuid(run.getUuid());
		Module module = run.getModule();
		run.setModule(module);
		run = run.store();
		run = Run.loadRunWithRuntimeParameters(run.getUuid());
		return run;
	}

	public class SyncLauncher {
		private Run run;
		private final User user;

		SyncLauncher(Run run, User user) {
			this.run = run;
			this.user = user;
		}

		public void run() throws NotFoundException, ValidationException {
			launch();
		}

		private void launch() {
			try {
				logger.info("Submitting asynchronous launch operation for run: "
						+ run.getUuid());

				switch (run.getType()) {
				case Orchestration:
				case Machine:
					runOrchestration();
					break;
				case Run:
					runImage();
					break;
				default:
					throw (new ServerExecutionEnginePluginException(
							"Cannot submit type: " + run.getType() + " yet!!"));
				}

			} catch (SlipStreamException e) {
				logger.severe("Error executing asynchronous launch operation");
				throw (new SlipStreamRuntimeException(e));
			}
		}

		private void runImage()
				throws ValidationException {
			Connector connector = ConnectorFactory.getCurrentConnector(user);
			try {
				connector.launch(run, user);
			} catch (SlipStreamException e) {
				abortRun(Run.MACHINE_NAME, e);
			}
		}

		private void runOrchestration()
				throws ValidationException {
			HashSet<String> cloudServicesList = RunFactory
					.getCloudServicesList(run);
			for (String cloudServiceName : cloudServicesList) {
				Connector connector = ConnectorFactory
						.getConnector(cloudServiceName);
				try {
					connector.launch(run, user);
				} catch (SlipStreamException e) {
					abortRun(connector.getOrchestratorName(run), e);
				}
			}
		}

		private void abortRun(String nodename, SlipStreamException e) {
			run = Run.abortOrReset(e.getMessage(), nodename, run.getUuid());
		}

	}
}
