package com.sixsq.slipstream.stats;

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

import java.util.List;

import org.simpleframework.xml.Root;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.DeploymentFactory;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Node;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;

/**
 * Unit test:
 * 
 * @see MeasurementsTest
 * 
 */
@Root(name = "measurements")
@SuppressWarnings("serial")
public class DeploymentMeasurements extends Measurements {

	@Override
	protected List<Measurement> populateSingle(Run run)
			throws ValidationException, NotFoundException, AbortException {

		String cloud;
		String nodename;

		for (String name : run.getOrchestrators()) {
			String firstPart = name
					.split(Run.ORCHESTRATOR_CLOUD_SERVICE_SEPARATOR)[0];
			String lastPart = name.substring(firstPart.length() + 1);
			cloud = lastPart;
			fill(run, name, Run.ORCHESTRATOR_NAME, cloud,
					ORCHESTRATOR_DEFAULT_CPU, ORCHESTRATOR_DEFAULT_RAM,
					ORCHESTRATOR_DEFAULT_STORAGE, getInstanceId(run, name));
		}

		for (Node node : DeploymentFactory.getNodes(run).values()) {

			ImageModule image = node.getImage();
			cloud = node.getCloudService();
			String effectiveCloud = RunFactory.getEffectiveCloudServiceName(cloud, run);

			nodename = node.getName();

			Measurement ms;
			for (int i = 1; i <= node.getMultiplicity(); i++) {
				ms = fill(run, RuntimeParameter.constructNodeName(nodename, i),
						image.getName(), effectiveCloud);
				ms.setIndex(i);
			}
		}

		return getMeasurments();
	}

	protected RunType getType() {
		return RunType.Orchestration;
	}
}
