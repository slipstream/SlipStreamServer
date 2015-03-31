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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.PersistenceUtil;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.persistence.VmRuntimeParameterMapping;
import com.sixsq.slipstream.usage.Record;

public class Collector {

	private static Logger logger = Logger.getLogger(Collector.class.getName());

	public static final int EXCEPTION_OCCURED = -2;
	public static final int NO_CREDENTIALS = -1;

	public static int collect(User user, Connector connector, int timeout) {
		int res = EXCEPTION_OCCURED;
		try {
			if (connector.isCredentialsSet(user)) {
				res = describeInstances(user, connector, timeout);
			} else {
				res = NO_CREDENTIALS;
			}
		} catch (ConfigurationException e) {
			logger.severe(e.getMessage());
		} catch (ValidationException e) {
			logger.warning(e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.warning(e.getMessage());
		} catch (Exception e) {
			logger.severe(e.getMessage());
		} 
		return res;
	}

	private static int describeInstances(User user, Connector connector, int timeout)
			throws ConfigurationException, ValidationException {
		user.addSystemParametersIntoUser(Configuration.getInstance()
				.getParameters());
		Properties props = new Properties();
		long startTime = System.currentTimeMillis();
		try {
			props = connector.describeInstances(user, timeout);
		} catch (SlipStreamException e) {
			logger.warning("Failed contacting cloud [SlipStreamException]: "
					+ getUserCloudLogRepr(user, connector) + " with '" + e.getMessage() + "'");
			return 0;
		} catch (SlipStreamRuntimeException e) {
			logger.warning("Failed contacting cloud [SlipStreamRuntimeException]: "
					+ getUserCloudLogRepr(user, connector) + " with '" + e.getMessage() + "'");
		} catch (Exception e) {
			logger.log(
					Level.SEVERE,
					"Error in describeInstances for "
							+ getUserCloudLogRepr(user, connector) + ": " + e.getMessage(), e);
		} finally {
			logTiming(user, connector, startTime, "describe VMs done.");
		}

		long describeStopTime = System.currentTimeMillis();
		int vmsPopulated = populateVmsForCloud(user, connector.getConnectorInstanceName(), props);
		logTiming(user, connector, describeStopTime, "populate DB VMs done.");
		return vmsPopulated;
	}

	private static int populateVmsForCloud(User user, String cloud, Properties idsAndStates) {
		List<Vm> vms = new ArrayList<Vm>();
		for (String instanceId : idsAndStates.stringPropertyNames()) {
			String state = (String) idsAndStates.get(instanceId);
			Vm vm = new Vm(instanceId, cloud, state, user.getName());
			vms.add(vm);
		}
		update(vms, user.getName(), cloud);
		return idsAndStates.size();
	}

	public static int update(List<Vm> newVms, String user, String cloud) {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Query q = em.createNamedQuery("byUserAndCloud");
		q.setParameter("user", user);
		q.setParameter("cloud", cloud);

		@SuppressWarnings("unchecked")
		List<Vm> oldVmList = q.getResultList();

		Map<String, Vm> filteredOldVmMap = new HashMap<String, Vm>();
		Map<String, Vm> newVmsMap = Vm.toMapByInstanceId(newVms);
		int removed = 0;
		for (Vm v : oldVmList) {
			String instanceId = v.getInstanceId();
			if (!newVmsMap.containsKey(instanceId)) {
				setVmstate(em, getMapping(v), "Unknown");
				em.remove(v);
				insertEnd(v.getInstanceId(), user, cloud);
				removed++;
			} else {
				filteredOldVmMap.put(instanceId, v);
			}
		}
		for (Vm v : newVmsMap.values()) {
			Vm old = filteredOldVmMap.get(v.getInstanceId());
			VmRuntimeParameterMapping m = getMapping(v);
			if (old == null) {
				Vm.setVmstate(em, m, v);
				setRunUuid(m, v);
				em.persist(v);
				Map<String, Double> metrics = createVmMetric();
				insertStart(v.getInstanceId(), user, cloud, metrics);
			} else {
				boolean merge = false;
				if (!v.getState().equals(old.getState())) {
					old.setState(v.getState());
					Vm.setVmstate(em, m, v);
					merge = true;
				}
				if (old.getRunUuid() == null) {
					setRunUuid(m, old);
					merge = true;
				}
				if (merge) {
					old = em.merge(old);
				}
			}
		}
		transaction.commit();
		em.close();
		return removed;
	}

	private static void insertEnd(String instanceId, String user, String cloud) {
		com.sixsq.slipstream.usage.Record r;
	}

	private static Map<String, Double> createVmMetric() {
		Map<String, Double> metrics = new HashMap<String, Double>();
		metrics.put("vm", 1.0);
		return metrics;
	}

	private static VmRuntimeParameterMapping getMapping(Vm v) {
		return VmRuntimeParameterMapping.find(v.getCloud(), v.getInstanceId());
	}


	private static void insertStart(String instanceId, String user, String cloud, Map<String, Double> metrics) {
		
	}

	private static void setVmstate(EntityManager em, VmRuntimeParameterMapping m, String vmstate) {
		if (m != null) {
			RuntimeParameter rp = m.getVmstateRuntimeParameter();
			rp.setValue(vmstate);
			em.merge(rp);
		}
	}

	private static void setRunUuid(VmRuntimeParameterMapping m, Vm v) {
		if (m != null) {
			RuntimeParameter rp = m.getVmstateRuntimeParameter();
			if (rp != null) {
				v.setRunUuid(rp.getContainer().getUuid());
			}
		}
	}

	private static void logTiming(User user, Connector connector, long startTime, String info) {
		long elapsed = System.currentTimeMillis() - startTime;
		logger.info(getUserCloudLogRepr(user, connector) + " (" + elapsed + " ms) : " + info);
	}

	private static String getUserCloudLogRepr(User user, Connector connector) {
		return "[" + user.getName() + "/" + connector.getConnectorInstanceName() + "]";
	}
}
