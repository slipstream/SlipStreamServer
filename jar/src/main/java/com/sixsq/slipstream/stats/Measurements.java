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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;

/**
 * Unit test:
 * 
 * @see MeasurementTest
 * 
 */
@Root(name = "measurements")
@SuppressWarnings("serial")
public class Measurements implements Serializable {

	@ElementList(inline = true, name = "ms")
	private List<Measurement> measurments = new ArrayList<Measurement>();

	public List<Measurement> populate() throws ConfigurationException,
			ValidationException {

		EntityManager em = PersistenceUtil.createEntityManager();

		try {
			List<Run> runs = Run.viewListAllActive(em);

			for (Run r : runs) {
				Measurements ms = MeasurementsFactory.get(r);
				getMeasurments().addAll(ms.populateSingle(r));
			}
		} finally {
			em.close();
		}

		return getMeasurments();
	}

	protected List<Measurement> populateSingle(Run run)
			throws ValidationException {
		return measurments;
	};

	public List<Measurement> getMeasurments() {
		return measurments;
	}

	protected void fill(Run run, String nodename, ImageModule image,
			String cloud) throws ValidationException {

		// might be default
		String effectiveCloud = run.getEffectiveCloudServiceName(cloud);
		
		Measurement m = new Measurement();

		m.setVm(run.getParameterValue(RuntimeParameter.constructParamName(
				nodename, RuntimeParameter.INSTANCE_ID_KEY), "Unknown"));
		m.setRun(run.getUuid());
		m.setNodeName(nodename);
		m.setModule(run.getModuleResourceUrl());
		m.setImage(image.getName());
		m.setType(run.getType());
		m.setCloud(effectiveCloud);
		m.setCpu(Integer.parseInt(getRuntimeParameterValue(ImageModule.CPU_KEY,
				run, nodename, effectiveCloud, "0")));
		m.setRam(Integer.parseInt(getRuntimeParameterValue(ImageModule.RAM_KEY,
				run, nodename, effectiveCloud, "0")));

		getMeasurments().add(m);
	}

	protected String getRuntimeParameterValue(String param, Run run,
			String nodename, String cloud, String defaultValue)
			throws ValidationException {
		String value;
		try {
			value = run.getRuntimeParameterValueIgnoreAbort(RuntimeParameter
					.constructParamName(nodename, cloud
							+ RuntimeParameter.PARAM_WORD_SEPARATOR + param));
		} catch (NotFoundException ex) {
			value = defaultValue;
		}
		return value;
	}
}
