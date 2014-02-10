package com.sixsq.slipstream.measurements;

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.util.Logger;
import com.sixsq.slipstream.util.SerializationUtil;

/**
 * Unit test:
 * 
 * @see MeasurementsTest
 * 
 */
@Root(name = "vms")
@SuppressWarnings("serial")
public class Measurements implements Serializable {

	protected static final int ORCHESTRATOR_DEFAULT_CPU = 1;
	protected static final int ORCHESTRATOR_DEFAULT_RAM = 1;
	protected static final int ORCHESTRATOR_DEFAULT_STORAGE = 1;
	protected static final int DEFAULT_STORAGE = 1;

	@ElementList(inline = true, name = "vm")
	private List<Measurement> measurments = new ArrayList<Measurement>();

	public List<Measurement> populate(User user) throws ConfigurationException,
			ValidationException, NotFoundException, AbortException {

		List<Vm> vms = Vm.list(user.getName());

		EntityManager em = PersistenceUtil.createEntityManager();

		try {
			List<Run> runs = Run.listAllActive(em, user);

			for (Run r : runs) {
				try {
					RunFactory.updateVmStatus(r, vms);
				} catch (SlipStreamException e) {
					Logger.warning(e.getMessage());
				}
				Measurements ms = MeasurementsFactory.get(r);
				getMeasurments().addAll(ms.populateSingle(r));
			}
		} finally {
			em.close();
		}

		return getMeasurments();
	}

	public String populateAsString(User user) throws ConfigurationException,
			ValidationException, NotFoundException, AbortException {
		populate(user);
		return SerializationUtil.toXmlString(this);
	}

	protected List<Measurement> populateSingle(Run run)
			throws ValidationException, NotFoundException, AbortException {
		return measurments;
	};

	public List<Measurement> getMeasurments() {
		return measurments;
	}

	protected Measurement fill(Run run, String nodename, String imagename,
			String cloud) throws ValidationException, NotFoundException,
			AbortException {

		int cpu = 0;
		int ram = 0;
		int storage = DEFAULT_STORAGE;
		String instanceid = "";

		try {
			cpu = Integer.parseInt(getRuntimeParameterValue(
					ImageModule.CPU_KEY, nodename, run, cloud, "0"));
		} catch (NumberFormatException e) {
		}

		try {
			ram = Integer.parseInt(getRuntimeParameterValue(
					ImageModule.RAM_KEY, nodename, run, cloud, "0"));
		} catch (NumberFormatException e) {
		}

		try {
			storage += Integer.parseInt(getRuntimeParameterValue(
					ImageModule.EXTRADISK_VOLATILE_PARAM, nodename, run));
		} catch (NotFoundException e) {
		} catch (NumberFormatException e) {
		}

		try {
			instanceid = getInstanceId(run, nodename);
		} catch (NotFoundException e) {
		}

		return fill(run, nodename, imagename, cloud, cpu, ram, storage,
				instanceid);
	}

	protected String getInstanceId(Run run, String nodename)
			throws AbortException, NotFoundException {
		return getRuntimeParameterValue(RuntimeParameter.INSTANCE_ID_KEY,
				nodename, run);
	}

	protected String getState(Run run, String nodename) throws AbortException,
			NotFoundException {
		return getRuntimeParameterValue(RuntimeParameter.STATE_KEY, nodename,
				run);
	}

	protected String getVmState(Run run, String nodename)
			throws AbortException, NotFoundException {
		return getRuntimeParameterValue(RuntimeParameter.STATE_VM_KEY,
				nodename, run);
	}

	protected Measurement fill(Run run, String nodename, String imagename,
			String cloud, int cpu, int ram, int storage, String instanceid)
			throws ValidationException, NotFoundException, AbortException {

		// might be 'default'
		String effectiveCloud = RunFactory.getEffectiveCloudServiceName(cloud,
				run);

		Measurement m = new Measurement();

		String state = getState(run, nodename);

		String vmstate = getVmState(run, nodename);

		m.setState(state);
		m.setVmState(vmstate);
		m.setInstanceId(instanceid);
		m.setRun(run.getUuid());
		m.setNodeName(nodename);
		m.setModule(run.getModuleResourceUrl());
		m.setImage(imagename);
		m.setType(run.getType());
		m.setCloud(effectiveCloud);
		m.setCpu(cpu);
		m.setRam(ram);
		m.setStorage(storage);
		m.setCreation(run.getCreation());
		m.setEnd(run.getEnd());
		m.setUser(run.getUser());

		m.setType(getType());

		getMeasurments().add(m);

		return m;
	}

	protected RunType getType() {
		return null;
	}

	private String getRuntimeParameterValue(String param, String node, Run run)
			throws AbortException, NotFoundException {
		return run.getRuntimeParameterValueIgnoreAbort(RuntimeParameter
				.constructParamName(node, param));
	}

	protected String getRuntimeParameterValue(String param, String node,
			Run run, String cloud, String defaultValue)
			throws ValidationException, NotFoundException {
		String value;
		String qualified = constructCloudQualifiedParamName(param, cloud);
		try {
			value = run.getRuntimeParameterValueIgnoreAbort(RuntimeParameter
					.constructParamName(node, qualified));
		} catch (NotFoundException ex) {
			value = defaultValue;
		}
		return value;
	}

	private String constructCloudQualifiedParamName(String param, String cloud) {
		// FIXME: need to be bound to the connectors
		String qualified = cloud + RuntimeParameter.PARAM_WORD_SEPARATOR
				+ param;
		return "".equals(cloud) ? param : qualified;
	}

}
