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

import java.util.Arrays;
import java.util.logging.Logger;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ServerExecutionEnginePluginException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.metrics.Metrics;
import com.sixsq.slipstream.metrics.MetricsTimer;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.User;

public class Launcher {

	private static Logger logger = Logger.getLogger(Launcher.class.getName());
	private static MetricsTimer launchTimer = Metrics.newTimer(Launcher.class, "launch");

	public static Run launch(Run run, User user) throws SlipStreamException {
		launchTimer.start();
		try {
			run = storeRunKeepModule(run);
			SyncLauncher sl = new SyncLauncher(run, user);
			sl.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			run = Run.abort(ex.getMessage(), run.getUuid());
			run = run.store();
		} finally {
			launchTimer.stop();
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

	public static class SyncLauncher {
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
				logger.info("Submitting asynchronous launch operation for run: " + run.getUuid());

				if (Arrays.asList(RunType.values()).contains(run.getType())) {
					launchRun();
				} else {
					throw (new ServerExecutionEnginePluginException(
							"Cannot submit type: " + run.getType() + " yet!!"));
				}

			} catch (SlipStreamException e) {
				logger.severe("Error executing asynchronous launch operation");
				throw (new SlipStreamRuntimeException(e));
			}
		}

		private void launchRun() throws ValidationException {
			for (String cloudServiceName : run.getCloudServiceNamesList()) {
				Connector connector = ConnectorFactory.getConnector(cloudServiceName);
				try {
					connector.launch(run, user);
				} catch (SlipStreamException e) {
					String orchestratorName = (run.getType() == RunType.Run) ? Run.MACHINE_NAME :
						connector.getOrchestratorName(run);
					abortRun(orchestratorName, e);
				}
			}
		}

		private void abortRun(String nodename, SlipStreamException e) {
			run = Run.abortOrReset(e.getMessage(), nodename, run.getUuid());
		}

	}
}
