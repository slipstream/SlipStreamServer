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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.metering.Metering;
import com.sixsq.slipstream.persistence.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

		user.addSystemParametersIntoUser(Configuration.getInstance().getParameters());

		Map<String, Properties> instances = new HashMap<String, Properties>();
		long startTime = System.currentTimeMillis();
		try {
			instances = connector.describeInstances(user, timeout);
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
		int vmsPopulated = populateVmsForCloud(user, connector, instances);
		logTiming(user, connector, describeStopTime, "populate DB VMs done.");
		return vmsPopulated;
	}

	private static int populateVmsForCloud(User user, Connector connector, Map<String, Properties> instances) {
		String cloud = connector.getConnectorInstanceName();

		List<Vm> cloudVms = new ArrayList<Vm>();
		for (Map.Entry<String, Properties> entry: instances.entrySet()) {
			String instanceId = entry.getKey();
			Properties properties = entry.getValue();
			String state = properties.getProperty(ConnectorBase.VM_STATE);

			Vm vm = new Vm(instanceId, cloud, state, user.getName(), connector.isVmUsable(state),
					properties.getProperty(ConnectorBase.VM_CPU), properties.getProperty(ConnectorBase.VM_RAM),
					properties.getProperty(ConnectorBase.VM_DISK), properties.getProperty(ConnectorBase.VM_INSTANCE_TYPE));
			cloudVms.add(vm);
		}

		update(cloudVms, user.getName(), cloud);

		return instances.size();
	}

	private static boolean vmHasRunUuid(Vm vm) {
		String runUuid = vm.getRunUuid();
		return runUuid != null && !runUuid.isEmpty();
	}

	private static boolean isVmRunOwnedByUser(VmRuntimeParameterMapping m, String user) {
		if (m != null) {
			RuntimeParameter rp = m.getVmstateRuntimeParameter();
			if (rp != null) {
				return user.equals(rp.getContainer().getUser());
			}
		}
		return false;
	}

	public static void update(List<Vm> cloudVms, String user, String cloud) {

		EntityManager em = PersistenceUtil.createEntityManager();
		List<Vm> dbVms = getDbVms(user, cloud, em);
		EntityTransaction transaction = em.getTransaction();

		VmsClassifier classifier = new VmsClassifier(cloudVms, dbVms);

		try {
			updateUsageRecords(classifier, user, cloud, em);
			updateGraphite(classifier, user, cloud, em);
			transaction.begin();
			updateDbVmsWithCloudVms(classifier, em);
			transaction.commit();
			em.close();
		} catch (Exception ex) {
			if (transaction != null) {
				transaction.rollback();
			}
			em.close();
			throw ex;
		}

	}

	private static void updateGraphite(VmsClassifier classifier, String user, String cloud, EntityManager em) {
		int cpu = 0;
		float ram = 0;
		float disk = 0;
		Map<String, Integer> instanceTypes = new HashMap<String, Integer>();

		for (Map.Entry<String, Map<String, Vm>> idDbCloud : classifier.stayingVms()) {

			Vm cloudVm = idDbCloud.getValue().get(VmsClassifier.CLOUD_VM);

			VmRuntimeParameterMapping cloudVmRtpMap = getMapping(cloudVm);

			if (isVmRunOwnedByUser(cloudVmRtpMap, user) && cloudVm.getIsUsable()) {
				Integer vmCpu = cloudVm.getCpu();
				if (vmCpu != null) {
					cpu += vmCpu;
				}

				Float vmRam = cloudVm.getRam();
				if (vmRam != null) {
					ram += vmRam;
				}

				Float vmDisk = cloudVm.getDisk();
				if (vmDisk != null) {
					disk += vmDisk;
				}

				String instanceType = cloudVm.getInstanceType();
				if (instanceType != null && !instanceType.isEmpty()) {
					Integer nb = 1;
					if (instanceTypes.containsKey(instanceType)) {
						nb = instanceTypes.get(instanceType) + 1;
					}
					instanceTypes.put(instanceType, nb);
				}
			}
		}

		Metering.populateVmMetrics(user, cloud, cpu, ram, disk, instanceTypes);
	}

	private static void updateUsageRecords(VmsClassifier classifier, String user, String cloud, EntityManager em) {

		for(Vm goneVm : classifier.goneVms()) {
			VmRuntimeParameterMapping goneVmRtpMap = getMapping(goneVm);
			// TODO unique place to check isVmRunOwnedByUser
			if (isVmRunOwnedByUser(goneVmRtpMap, user)) {
				// TODO new signature insertEndAllMetrics??
				UsageRecorder.insertEnd(goneVm.getInstanceId(), user, cloud, UsageRecorder.createVmMetrics(goneVm));
			}
		}

		for(Vm newVm : classifier.newVms()) {
			VmRuntimeParameterMapping newVmRtpMap = getMapping(newVm);
			if (newVm.getIsUsable() && isVmRunOwnedByUser(newVmRtpMap, user)) {
				UsageRecorder.insertStart(newVm.getInstanceId(), user, cloud, UsageRecorder.createVmMetrics(newVm));
			}
		}

		for(Map.Entry<String, Map<String, Vm>> idDbCloud : classifier.stayingVms()) {

			Vm cloudVm = idDbCloud.getValue().get(VmsClassifier.CLOUD_VM);
			Vm dbVm  = idDbCloud.getValue().get(VmsClassifier.DB_VM);
			VmRuntimeParameterMapping cloudVmRtpMap = getMapping(cloudVm);
			if(!isVmRunOwnedByUser(cloudVmRtpMap, user)) {
				continue;
			}

			boolean usabilityChanged = cloudVm.getIsUsable() != dbVm.getIsUsable();
			boolean runUuidDiscoveredAndUsable = !vmHasRunUuid(dbVm) && vmHasRunUuid(cloudVm) && cloudVm.getIsUsable();
			if (usabilityChanged ||runUuidDiscoveredAndUsable) {
				if (cloudVm.getIsUsable()) {
					UsageRecorder.insertStart(cloudVm.getInstanceId(), user, cloud, UsageRecorder.createVmMetrics(cloudVm));
				} else {
					UsageRecorder.insertEnd(cloudVm.getInstanceId(), user, cloud, UsageRecorder.createVmMetrics(cloudVm));
				}
			}

			if (usabilityChanged && cloudVm.getIsUsable()) {
				boolean ramHasChanged = cloudVm.getRam() != dbVm.getRam();
				if (ramHasChanged) {
					UsageRecorder.insertRestart(cloudVm.getInstanceId(), user, cloud, new UsageMetric("ram", cloudVm.getRam()));
				}
			}
		}
	}

	private static void updateDbVmsWithCloudVms(VmsClassifier classifier, EntityManager em) {

		for(Vm goneVm : classifier.goneVms()) {
			VmRuntimeParameterMapping goneVmRtpMap = getMapping(goneVm);
			setVmStateRuntimeParameter(em, goneVmRtpMap, "Unknown");
			em.remove(goneVm);
		}

		for(Vm newVm : classifier.newVms()) {
			VmRuntimeParameterMapping newVmRtpMap = getMapping(newVm);
			updateVmFromRuntimeParametersMappings(newVm, newVmRtpMap);
			setVmStateRuntimeParameter(em, newVmRtpMap, newVm);
			em.persist(newVm);
		}

		for(Map.Entry<String, Map<String, Vm>> idDbCloud : classifier.stayingVms()) {
			Vm cloudVm = idDbCloud.getValue().get(VmsClassifier.CLOUD_VM);
			Vm dbVm  = idDbCloud.getValue().get(VmsClassifier.DB_VM);

			VmRuntimeParameterMapping cloudVmRtpMap = getMapping(cloudVm);
			updateVmFromRuntimeParametersMappings(cloudVm, cloudVmRtpMap);

			boolean merge = false;
			boolean vmStateHasChanged = !cloudVm.getState().equals(dbVm.getState());
			if (vmStateHasChanged) {
				dbVm.setState(cloudVm.getState());
				dbVm.setIsUsable(cloudVm.getIsUsable());
				// DB update
				setVmStateRuntimeParameter(em, cloudVmRtpMap, cloudVm);
				merge = true;
			} else {
				// DB update
				setVmstateIfNotYetSet(em, cloudVmRtpMap, cloudVm);
			}

			// TODO : extract methods
			// VM coordinates related
			if (cloudVmRtpMap != null) {
				if (dbVm.getRunUuid() == null) {
					setRunUuid(dbVm, cloudVmRtpMap);
					merge = true;
				}
				if (dbVm.getRunOwner() == null) {
					setRunOwner(dbVm, cloudVmRtpMap);
					merge = true;
				}
				if (dbVm.getIp() == null) {
					// DB select
					setIp(dbVm, cloudVmRtpMap);
					merge = true;
				}
				if (dbVm.getName() == null) {
					setName(dbVm, cloudVmRtpMap);
					merge = true;
				}
				if (dbVm.getNodeName() == null) {
					setNodeName(dbVm, cloudVmRtpMap);
					merge = true;
				}
				if (dbVm.getNodeInstanceId() == null) {
					setNodeInstanceId(dbVm, cloudVmRtpMap);
					merge = true;
				}
			}
			// TODO : extract methods
			// VM metrics related
			if (cloudVm.getCpu() != null && !cloudVm.getCpu().equals(dbVm.getCpu())) {
				dbVm.setCpu(cloudVm.getCpu());
				merge = true;
			}
			if (cloudVm.getRam() != null && !cloudVm.getRam().equals(dbVm.getRam())) {
				dbVm.setRam(cloudVm.getRam());
				merge = true;
			}
			if (cloudVm.getDisk() != null && !cloudVm.getDisk().equals(dbVm.getDisk())) {
				dbVm.setDisk(cloudVm.getDisk());
				merge = true;
			}
			if (cloudVm.getInstanceType() != null && !cloudVm.getInstanceType().equals(dbVm.getInstanceType())) {
				dbVm.setInstanceType(cloudVm.getInstanceType());
				merge = true;
			}
			if (merge) {
				em.merge(dbVm);
			}

		}
	}

	private static void updateVmFromRuntimeParametersMappings(Vm v, VmRuntimeParameterMapping m) {
		setIp(v, m);
		setName(v, m);
		setRunUuid(v, m);
		setRunOwner(v, m);
		setNodeName(v, m);
		setNodeInstanceId(v, m);
	}

	private static List<Vm> getDbVms(String user, String cloud, EntityManager em) {
		Query q = em.createNamedQuery("byUserAndCloud");
		q.setParameter("user", user);
		q.setParameter("cloud", cloud);

		return q.getResultList();
	}

	private static VmRuntimeParameterMapping getMapping(Vm v) {
		return VmRuntimeParameterMapping.find(v.getCloud(), v.getInstanceId());
	}

	private static void setVmStateRuntimeParameter(EntityManager em, VmRuntimeParameterMapping m, Vm v) {
		setVmStateRuntimeParameter(em, m, v.getState());
	}

	private static void setVmStateRuntimeParameter(EntityManager em, VmRuntimeParameterMapping m, String vmstate) {
		if (m != null) {
			RuntimeParameter rp = m.getVmstateRuntimeParameter();
			rp.setValue(vmstate);
			em.merge(rp);
		}
	}

	private static void setVmstateIfNotYetSet(EntityManager em, VmRuntimeParameterMapping m, Vm v) {
		if (m != null) {
			RuntimeParameter rp = m.getVmstateRuntimeParameter();
			if (!rp.isSet()) {
				setVmStateRuntimeParameter(em, m, v);
			}
		}
	}

	private static void setRunUuid(Vm v, VmRuntimeParameterMapping m) {
		if (m != null) {
			v.setRunUuid(m.getRunUuid());
		}
	}

	private static void setRunOwner(Vm v, VmRuntimeParameterMapping m) {
		if (m != null) {
			v.setRunOwner(m.getRunOwner());
		}
	}

	private static void setIp(Vm v, VmRuntimeParameterMapping m) {
		if (m != null) {
			RuntimeParameter rp = m.getHostnameRuntimeParameter();
			if (rp.isSet()) {
				v.setIp(rp.getValue());
			}
		}
	}

	private static void setName(Vm v, VmRuntimeParameterMapping m) {
		if (m != null) {
			v.setName(m.getName());
		}
	}

	private static void setNodeName(Vm v, VmRuntimeParameterMapping m) {
		if (m != null) {
			v.setNodeName(m.getNodeName());
		}
	}

	private static void setNodeInstanceId(Vm v, VmRuntimeParameterMapping m) {
		if (m != null) {
			v.setNodeInstanceId(m.getNodeInstanceId());
		}
	}

	private static void logTiming(User user, Connector connector, long startTime, String info) {
		long elapsed = System.currentTimeMillis() - startTime;
		logger.finer(getUserCloudLogRepr(user, connector) + " (" + elapsed + " ms) : " + info);
	}

	private static String getUserCloudLogRepr(User user, Connector connector) {
		return "[" + user.getName() + "/" + connector.getConnectorInstanceName() + "]";
	}
}

