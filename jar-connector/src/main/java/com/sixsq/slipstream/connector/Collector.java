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

		List<Vm> vms = new ArrayList<Vm>();
		for (Map.Entry<String, Properties> entry: instances.entrySet()) {
			String instanceId = entry.getKey();
			Properties properties = entry.getValue();
			String state = properties.getProperty(ConnectorBase.VM_STATE);

			Vm vm = new Vm(instanceId, cloud, state, user.getName(), connector.isVmUsable(state),
					properties.getProperty(ConnectorBase.VM_CPU), properties.getProperty(ConnectorBase.VM_RAM),
					properties.getProperty(ConnectorBase.VM_DISK), properties.getProperty(ConnectorBase.VM_INSTANCE_TYPE));
			vms.add(vm);
		}

		update(vms, user.getName(), cloud);

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

	public static int update(List<Vm> newVms, String user, String cloud) {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Query q = em.createNamedQuery("byUserAndCloud");
		q.setParameter("user", user);
		q.setParameter("cloud", cloud);

		@SuppressWarnings("unchecked")
		List<Vm> oldVmList = q.getResultList();

		int cpu = 0;
		float ram = 0;
		float disk = 0;
		Map<String, Integer> instanceTypes = new HashMap<String, Integer>();

		Map<String, Vm> filteredOldVmMap = new HashMap<String, Vm>();
		Map<String, Vm> newVmsMap = toMapByInstanceId(newVms);
		int removed = 0;
		for (Vm vm : oldVmList) {
			String instanceId = vm.getInstanceId();
			if (!newVmsMap.containsKey(instanceId)) {
				VmRuntimeParameterMapping m = getMapping(vm);
				setVmstate(em, m, "Unknown");
				em.remove(vm);
				removed++;

				if (isVmRunOwnedByUser(m, user)) {
					UsageRecorder.insertEnd(instanceId, user, cloud);

					String instanceType = vm.getInstanceType();
					if (instanceType != null && !instanceType.isEmpty() && !instanceTypes.containsKey(instanceType)){
						instanceTypes.put(instanceType, 0);
					}
				}
			} else {
				filteredOldVmMap.put(instanceId, vm);
			}
		}

		for (Vm v : newVmsMap.values()) {
			Vm old = filteredOldVmMap.get(v.getInstanceId());
			VmRuntimeParameterMapping m = getMapping(v);

			setIp(m, v);
			setName(m, v);
			setRunUuid(m, v);
			setRunOwner(m, v);
			setNodeName(m, v);
			setNodeInstanceId(m, v);

			if (old == null) {
				setVmstate(em, m, v);

				em.persist(v);

				if (v.getIsUsable() && isVmRunOwnedByUser(m, user)) {
					UsageRecorder.insertStart(v.getInstanceId(), user, cloud, UsageRecorder.createVmMetrics(v));
				}
			} else {
				boolean merge = false;

				if (((v.getIsUsable() != old.getIsUsable()) || (!vmHasRunUuid(old) && vmHasRunUuid(v) && v.getIsUsable()))
						&& isVmRunOwnedByUser(m, user)) {
					if (v.getIsUsable()) {
						UsageRecorder.insertStart(v.getInstanceId(), user, cloud, UsageRecorder.createVmMetrics(v));
					} else {
						UsageRecorder.insertEnd(v.getInstanceId(), user, cloud);

						String instanceType = v.getInstanceType();
						if (instanceType != null && !instanceType.isEmpty() && !instanceTypes.containsKey(instanceType)){
							instanceTypes.put(instanceType, 0);
						}
					}
				}

				if (!v.getState().equals(old.getState())) {
					old.setState(v.getState());
					old.setIsUsable(v.getIsUsable());
					setVmstate(em, m, v);
					merge = true;
				} else {
					setVmstateIfNotYetSet(em, m, v);
				}
				if (old.getRunUuid() == null) {
					setRunUuid(m, old);
					merge = true;
				}
				if (old.getRunOwner() == null) {
					setRunOwner(m, old);
					merge = true;
				}
				if (old.getIp() == null) {
					setIp(m, old);
					merge = true;
				}
				if (old.getName() == null) {
					setName(m, old);
					merge = true;
				}
				if (old.getNodeName() == null) {
					setNodeName(m, old);
					merge = true;
				}
				if (old.getNodeInstanceId() == null) {
					setNodeInstanceId(m, old);
					merge = true;
				}
				if (merge) {
					em.merge(old);
				}
			}

			if (isVmRunOwnedByUser(m, user) && v.getIsUsable()) {
				Integer vmCpu = v.getCpu();
				if (vmCpu != null)
					cpu += vmCpu;

				Float vmRam = v.getRam();
				if (vmRam != null)
					ram += vmRam;

				Float vmDisk = v.getDisk();
				if (vmDisk != null)
					disk += vmDisk;

				String instanceType = v.getInstanceType();
				if (instanceType != null && !instanceType.isEmpty()){
					Integer nb = 1;
					if (instanceTypes.containsKey(instanceType)) {
						nb = instanceTypes.get(instanceType) + 1;
					}
					instanceTypes.put(instanceType, nb);
				}
			}

		}
		transaction.commit();
		em.close();

		Metering.populateVmMetrics(user, cloud, cpu, ram, disk, instanceTypes);

		return removed;
	}

	private static VmRuntimeParameterMapping getMapping(Vm v) {
		return VmRuntimeParameterMapping.find(v.getCloud(), v.getInstanceId());
	}

	private static void setVmstate(EntityManager em, VmRuntimeParameterMapping m, Vm v) {
		setVmstate(em, m, v.getState());
	}

	private static void setVmstate(EntityManager em, VmRuntimeParameterMapping m, String vmstate) {
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
				setVmstate(em, m, v);
			}
		}
	}

	private static void setRunUuid(VmRuntimeParameterMapping m, Vm v) {
		if (m != null) {
			v.setRunUuid(m.getRunUuid());
		}
	}

	private static void setRunOwner(VmRuntimeParameterMapping m, Vm v) {
		if (m != null) {
			v.setRunOwner(m.getRunOwner());
		}
	}

	private static void setIp(VmRuntimeParameterMapping m, Vm v) {
		if (m != null) {
			RuntimeParameter rp = m.getHostnameRuntimeParameter();
			if (rp.isSet()) {
				v.setIp(rp.getValue());
			}
		}
	}

	private static void setName(VmRuntimeParameterMapping m, Vm v) {
		if (m != null) {
			v.setName(m.getName());
		}
	}

	private static void setNodeName(VmRuntimeParameterMapping m, Vm v) {
		if (m != null) {
			v.setNodeName(m.getNodeName());
		}
	}

	private static void setNodeInstanceId(VmRuntimeParameterMapping m, Vm v) {
		if (m != null) {
			v.setNodeInstanceId(m.getNodeInstanceId());
		}
	}

	/**
	 * This method assumes that the input VMs correspond to a single cloud.
	 * Otherwise, duplicate instance ids would overwrite each other.
	 *
	 * @param vms
	 *            for a single cloud
	 * @return mapped VMs by instance id
	 */
	public static Map<String, Vm> toMapByInstanceId(List<Vm> vms) {
		Map<String, Vm> map = new HashMap<String, Vm>();
		for (Vm v : vms) {
			map.put(v.getInstanceId(), v);
		}
		return map;
	}

	private static void logTiming(User user, Connector connector, long startTime, String info) {
		long elapsed = System.currentTimeMillis() - startTime;
		logger.finer(getUserCloudLogRepr(user, connector) + " (" + elapsed + " ms) : " + info);
	}

	private static String getUserCloudLogRepr(User user, Connector connector) {
		return "[" + user.getName() + "/" + connector.getConnectorInstanceName() + "]";
	}
}
