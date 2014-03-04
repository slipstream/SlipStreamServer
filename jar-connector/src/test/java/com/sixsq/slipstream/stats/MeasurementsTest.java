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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.measurements.Measurement;
import com.sixsq.slipstream.measurements.Measurements;
import com.sixsq.slipstream.persistence.CloudImageIdentifier;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.run.RunTestBase;
import com.sixsq.slipstream.statemachine.States;
import com.sixsq.slipstream.util.CommonTestUtil;

public class MeasurementsTest extends RunTestBase {

	@BeforeClass
	public static void setupClass() throws InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException,
			ConfigurationException, ValidationException, NotFoundException {

		CommonTestUtil
				.resetAndLoadConnector(com.sixsq.slipstream.connector.local.LocalConnector.class);

		setupImages();

		imagebuildme = updateImageForLocalConnector(imagebuildme, 2, 4);
		image = updateImageForLocalConnector(image, 22, 44);

		setupDeployments();
		imageForDeployment1 = updateImageForDeployment(imageForDeployment1, 4,
				8);
		imageForDeployment1 = imageForDeployment1.store();
		imageForDeployment2 = updateImageForDeployment(imageForDeployment2, 44,
				88);
		imageForDeployment2 = imageForDeployment2.store();

		createUser();
		
		CommonTestUtil.addSshKeys(user);
		
		removeRuns();
		
		CommonTestUtil.createDeployment();
	}

	private static void removeRuns() throws ValidationException {
		for (Run r : Run.listAll()) {
			r.remove();
		}
	}

	private static ImageModule updateImageForLocalConnector(ImageModule image,
			int cpu, int ram) throws ValidationException {
		image.setParameter(new ModuleParameter(Parameter.constructKey(
				cloudServiceName, Run.CPU_PARAMETER_NAME), String.valueOf(cpu),
				""));
		image.setParameter(new ModuleParameter(Parameter.constructKey(
				cloudServiceName, Run.RAM_PARAMETER_NAME), String.valueOf(ram),
				""));

		return image.store();
	}

	private static ImageModule updateImageForDeployment(ImageModule image,
			int cpu, int ram) throws ValidationException {
		image = updateImageForLocalConnector(image, cpu, ram);
		image.getCloudImageIdentifiers().add(
				new CloudImageIdentifier(image, cloudServiceName, "abc"));
		return image.store();
	}

	@After
	public void tearDown() throws ConfigurationException, ValidationException {
		removeRuns();
	}

	@Test
	public void computeImageBuildRun() throws SlipStreamException {

		createACoupleOfImageBuildRuns(imagebuildme);

		Measurements ms = new Measurements();
		ms.populate(user);

		assertEquals(4, ms.getMeasurments().size());

		// first orchestrator
		Measurement m = ms.getMeasurments().get(0);

		assertEquals(1, m.getCpu());
		assertEquals(1, m.getRam());

		// first machine
		m = ms.getMeasurments().get(1);

		assertEquals(2, m.getCpu());
		assertEquals(4, m.getRam());

		// second machine
		m = ms.getMeasurments().get(3);

		assertEquals(2, m.getCpu());
		assertEquals(4, m.getRam());
	}

	@Test
	public void computeDeploymentRun() throws SlipStreamException {

		createACoupleOfDeploymentRuns(deployment);

		Measurements ms = new Measurements();
		ms.populate(user);

		// 2 x (2 VMs + 1 orchestrator)
		assertEquals(6, ms.getMeasurments().size());

		Measurement m = ms.getMeasurments().get(0);

		assertEquals(1, m.getCpu());
		assertEquals(1, m.getRam());

		m = ms.getMeasurments().get(1);

		assertEquals(4, m.getCpu());
		assertEquals(8, m.getRam());

		m = ms.getMeasurments().get(2);

		assertEquals(44, m.getCpu());
		assertEquals(88, m.getRam());
	}

	@Test
	public void computeSimpleRun() throws SlipStreamException {

		createACoupleOfSimpleRuns(image);

		Measurements ms = new Measurements();
		ms.populate(user);

		assertEquals(2, ms.getMeasurments().size());

		Measurement m = ms.getMeasurments().get(1);

		assertEquals(22, m.getCpu());
		assertEquals(44, m.getRam());
	}

	private void createACoupleOfImageBuildRuns(Module module)
			throws SlipStreamException {
		createAndStoreRun(module, user.getName(), RunType.Machine, States.Running);
		createAndStoreRun(module, user.getName(), RunType.Machine, States.Running);
	}

	private void createACoupleOfDeploymentRuns(Module module)
			throws SlipStreamException {
		createAndStoreRun(module, user.getName(), RunType.Orchestration, States.Running);
		createAndStoreRun(module, user.getName(), RunType.Orchestration, States.Running);
	}

	private void createACoupleOfSimpleRuns(Module module)
			throws SlipStreamException {
		createAndStoreRun(module, user.getName(), RunType.Run, States.Running);
		createAndStoreRun(module, user.getName(), RunType.Run, States.Running);
	}

}
