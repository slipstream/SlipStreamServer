package com.sixsq.slipstream.persistence;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;

import org.simpleframework.xml.Attribute;

/**
 * Unit test:
 *
 * @see VmTest
 *
 */
@Entity
@NamedQueries({
		@NamedQuery(name = "byUser", query = "SELECT v FROM Vm v WHERE v.user_ = :user"),
		@NamedQuery(name = "byUserAndCloud", query = "SELECT v.instanceId, v FROM Vm v WHERE v.user_ = :user AND v.cloud = :cloud"),
		@NamedQuery(name = "byRun", query = "SELECT v.cloud, v FROM Vm v WHERE v.runUuid = :run"),
		@NamedQuery(name = "usageByUser", query = "SELECT v.cloud, COUNT(v.runUuid) FROM Vm v WHERE v.user_ = :user AND v.state IN ('Running', 'running', 'On', 'on', 'active', 'Active') AND v.runUuid IS NOT NULL AND v.runUuid <> 'Unknown' GROUP BY v.cloud ORDER BY v.cloud") })
public class Vm {

	public final static String RESOURCE_URL_PREFIX = "vms/";

	@Id
	@GeneratedValue
	Long id;

	@Attribute
	private String cloud;

	@Attribute(name = "user")
	private String user_;

	@Attribute
	private String instanceId;

	@Attribute
	private String state;

	@Attribute
	private Date measurement;

	@Attribute(required = false)
	private String runUuid;

	@SuppressWarnings("unused")
	private Vm() {
	}

	public Vm(String instanceid, String cloud, String state, String user) {
		this.instanceId = instanceid;
		this.cloud = cloud;
		this.state = state;
		this.user_ = user;
		measurement = new Date();
	}

	@SuppressWarnings("unchecked")
	public static List<Vm> list(String user) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("byUser");
		q.setParameter("user", user);
		List<Vm> vms = q.getResultList();
		em.close();
		return vms;
	}

	public static int update(List<Vm> newVms, String user, String cloud) {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Query q = em.createNamedQuery("byUserAndCloud");
		q.setParameter("user", user);
		q.setParameter("cloud", cloud);
		List<?> oldVmList = q.getResultList();
		Map<String, Vm> filteredOldVmMap = new HashMap<String, Vm>();
		Map<String, Vm> newVmsMap = toMapByInstanceId(newVms);
		int removed = 0;
		for (Object o : oldVmList) {
			String instanceId = (String) ((Object[]) o)[0];
			Vm vm = (Vm) ((Object[]) o)[1];
			if (!newVmsMap.containsKey(instanceId)) {
				setVmstate(em, getMapping(vm), "Unknown");
				em.remove(vm);
				removed++;
			} else {
				filteredOldVmMap.put(instanceId, vm);
			}
		}
		for (Vm v : newVmsMap.values()) {
			Vm old = filteredOldVmMap.get(v.getInstanceId());
			VmRuntimeParameterMapping m = getMapping(v);
			if (old == null) {
				setVmstate(em, m, v);
				setRunUuid(m, v);
				em.persist(v);
			} else {
				boolean merge = false;
				if (!v.getState().equals(old.getState())) {
					old.setState(v.getState());
					setVmstate(em, m, v);
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

	private static void setRunUuid(VmRuntimeParameterMapping m, Vm v) {
		if (m != null) {
			RuntimeParameter rp = m.getVmstateRuntimeParameter();
			if (rp != null) {
				v.setRunUuid(rp.getContainer().getUuid());
			}
		}
	}

	public static Map<String, Integer> usage(String user) {
		EntityManager em = PersistenceUtil.createEntityManager();

		List<?> res = em.createNamedQuery("usageByUser").setParameter("user", user).getResultList();
		em.close();

		Map<String, Integer> usageData = new HashMap<String, Integer>(res.size());
		for (Object object : res) {
			usageData.put((String) ((Object[]) object)[0], ((Long) ((Object[]) object)[1]).intValue());
		}

		return usageData;
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

	/**
	 * Maps the VMs into list per cloud.
	 *
	 * @param vms
	 *            for all or any cloud
	 * @return map of VMs where the key is a cloud, and the value a list of
	 *         corresponding VMs for that cloud.
	 */
	public static Map<String, List<Vm>> toMapByCloud(List<Vm> vms) {
		Map<String, List<Vm>> map = new HashMap<String, List<Vm>>();
		for (Vm v : vms) {
			List<Vm> forCloud = map.get(v.getCloud());
			if (forCloud == null) {
				forCloud = new ArrayList<Vm>();
				map.put(v.getCloud(), forCloud);
			}
			forCloud.add(v);
		}
		return map;
	}

	/**
	 * Extract for a give run id, the VMs, grouped by cloud
	 *
	 * @param runUuid
	 * @return map of vms grouped by cloud
	 */
	public static Map<String, List<Vm>> listByRun(String runUuid) {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Query q = em.createNamedQuery("byRun");
		q.setParameter("run", runUuid);

		List<?> vmList = q.getResultList();
		Map<String, List<Vm>> vmMap = new HashMap<String, List<Vm>>();
		for (Object o : vmList) {
			String cloud = (String) ((Object[]) o)[0];
			Vm vm = (Vm) ((Object[]) o)[1];
			List<Vm> vms = vmMap.get(cloud);
			if (vms == null) {
				vms = new ArrayList<Vm>();
				vmMap.put(cloud, vms);
			}
			vms.add(vm);
		}

		transaction.commit();
		em.close();
		return vmMap;
	}

	private void setState(String state) {
		this.state = state;
	}

	public String getCloud() {
		return cloud;
	}

	public String getUser() {
		return user_;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public String getState() {
		return state;
	}

	public Date getMeasurement() {
		return measurement;
	}

	public String getRunUuid() {
		return runUuid;
	}

	public void setRunUuid(String runUuid) {
		this.runUuid = runUuid;
	}

	public void remove() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Vm fromDb = em.find(Vm.class, id);
		if (fromDb != null) {
			em.remove(fromDb);
		}
		transaction.commit();
		em.close();
	}
}
