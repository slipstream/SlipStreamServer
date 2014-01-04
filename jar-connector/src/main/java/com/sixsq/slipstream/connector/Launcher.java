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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;

public class Launcher {

	private static final String ID_KEY = "id";
	private static final String IP_KEY = "ip";

	private static Logger logger = Logger.getLogger(Launcher.class.getName());

	public static Run launch(Run run, User user) throws SlipStreamException {

		try {
			run = storeRunKeepModule(run);
			SyncLauncher sl = (new Launcher()).new SyncLauncher(run, user);
			sl.run();
		} catch (Exception ex) {
			run = Run.abort(ex.getMessage(), run.getUuid());
		}
		return run;
	}

	private static Run storeRunKeepModule(Run run) throws ValidationException {
		Module module = run.getModule();
		run = run.store();
		run.setModule(module);
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

			Map<String, Properties> idsAndIps = launch();

			setIdsAndIps(idsAndIps);
		}

		private void setIdsAndIps(Map<String, Properties> idsAndIps)
				throws NotFoundException, ValidationException {

			for (Map.Entry<String, Properties> entry : idsAndIps.entrySet()) {

				RuntimeParameter rp;
				String key = entry.getKey();

				String idParamName = key
						+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
						+ RuntimeParameter.INSTANCE_ID_KEY;
				rp = RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
						idParamName);
				rp.setValue(idsAndIps.get(key).getProperty(ID_KEY));

				String ipParamName = key
						+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
						+ RuntimeParameter.HOSTNAME_KEY;
				rp = RuntimeParameter.loadFromUuidAndKey(run.getUuid(),
						ipParamName);
				rp.setValue(idsAndIps.get(key).getProperty(IP_KEY));

			}
		}

		private Map<String, Properties> launch() {
			Map<String, Properties> idsAndIps = new HashMap<String, Properties>();

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

		private void runDeployment(Map<String, Properties> idsAndIps)
				throws ValidationException {
			HashSet<String> cloudServicesList = RunFactory
					.getCloudServicesList(run);
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

		private void assembleIdsAndIps(Map<String, Properties> idsAndIps,
				Connector connector) throws NotFoundException {
			String id = run.getRuntimeParameterValueIgnoreAbort(connector
					.getOrchestratorName(run)
					+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
					+ RuntimeParameter.INSTANCE_ID_KEY);
			String ip = run.getRuntimeParameterValueIgnoreAbort(connector
					.getOrchestratorName(run)
					+ RuntimeParameter.NODE_PROPERTY_SEPARATOR
					+ RuntimeParameter.HOSTNAME_KEY);
			Properties props = new Properties();
			props.put(ID_KEY, id);
			props.put(IP_KEY, ip);
			idsAndIps.put(connector.getOrchestratorName(run), props);
		}

		private void abortRun(String nodename, SlipStreamException e) {
			run = run.store();
			run = Run.abortOrReset(e.getMessage(), nodename, run.getUuid());
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
