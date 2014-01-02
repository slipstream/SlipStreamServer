package com.sixsq.slipstream.run;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;

public class Launcher {

	private static ExecutorService executor = Executors.newCachedThreadPool();
	private static Logger logger = Logger.getLogger(Launcher.class.getName());

	public Run launch(Run run, User user) throws SlipStreamException {

		validateUserParameters(user);

		run = storeRunKeepModule(run);

		launchAsync(run, user);

		return run;
	}

	private Run storeRunKeepModule(Run run) throws ValidationException {
		Module module = run.getModule();
		run = run.store();
		run.setModule(module);
		return run;
	}

	private void validateUserParameters(User user) {

	}

	private void launchAsync(Run run, User user) {
		executor.execute(new AsyncLauncher(run, user));
	}

	public class AsyncLauncher implements Runnable {
		private Run run;
		private final User user;

		AsyncLauncher(Run run, User user) {
			this.run = run;
			this.user = user;
		}

		@Override
		public void run() {
			
			Map<String, String> idsAndIps = launch();

			setIdsAndIps(idsAndIps);
		}

		private void setIdsAndIps(Map<String, String> idsAndIps) {
			try {
				EntityManager em = PersistenceUtil.createEntityManager();
				run = Run.load(run.getResourceUri(), em);

				for (Map.Entry<String, String> entry : idsAndIps.entrySet()) {
					String key = entry.getKey();
					String value = entry.getValue();
					String[] ipid = value.split(",");
					run.updateRuntimeParameter(key
							+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
							+ RuntimeParameter.INSTANCE_ID_KEY, ipid[0]);
					run.updateRuntimeParameter(key
							+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
							+ RuntimeParameter.HOSTNAME_KEY, ipid[1]);
				}

				run = run.store();
			} catch (Exception e) {
				logger.severe("Error setting IP and ID");
				e.printStackTrace();
			}
		}

		private Map<String, String> launch() {
			Map<String, String> idsAndIps = new HashMap<String, String>();

			try {
				logger.info("Submitting asynchronous launch operation for run: "
						+ run.getUuid());

				if (run.getCategory() == ModuleCategory.Deployment) {
					runDeployment(idsAndIps);
				} else {
					runOther();
				}

			} catch (SlipStreamException e) {
				logger.severe("Error executing asynchronous launch operation");
				throw (new SlipStreamRuntimeException(e));
			} finally {
				run = run.store();
			}
			return idsAndIps;
		}

		private void runDeployment(Map<String, String> idsAndIps)
				throws ValidationException {
			HashSet<String> cloudServicesList = RunFactory.getCloudServicesList(run);
			for (String cloudServiceName : cloudServicesList) {
				Connector connector = ConnectorFactory
						.getConnector(cloudServiceName);
				try {
					connector.launch(run, user);
					assembleIdsAndIps(idsAndIps, connector);
				} catch (SlipStreamException e) {
					abortRun(connector.getOrchestratorName(run), e);
				}
			}
		}

		private void abortRun(String nodename, SlipStreamException e) {
			run = run.store();
			run = Run.abortOrReset(e.getMessage(),
					nodename, run.getUuid());
		}

		private void assembleIdsAndIps(Map<String, String> idsAndIps,
				Connector connector) {
			try {
				String id = run.getRuntimeParameterValue(connector
						.getOrchestratorName(run)
						+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
						+ RuntimeParameter.INSTANCE_ID_KEY);
				String ip = run.getRuntimeParameterValue(connector
						.getOrchestratorName(run)
						+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
						+ RuntimeParameter.HOSTNAME_KEY);
				idsAndIps.put(connector.getOrchestratorName(run), id
						+ "," + ip);
			} catch (Exception e) {
				logger.severe("Error getting IP and ID");
				e.printStackTrace();
			}
		}

		private void runOther() throws ValidationException {
			Connector connector = ConnectorFactory.getCurrentConnector(user);
			try {
				connector.launch(run, user);
			} catch (SlipStreamException e) {
				abortRun(Run.MACHINE_NAME, e);
			}
		}

	}
}
