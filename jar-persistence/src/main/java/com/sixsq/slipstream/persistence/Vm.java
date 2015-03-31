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
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.UniqueConstraint;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.simpleframework.xml.Attribute;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;

/**
 * Unit test:
 *
 * @see VmTest
 *
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames={"cloud", "instanceId", "user_"})})
@NamedQueries({
		@NamedQuery(name = "byUserAndCloud", query = "SELECT v FROM Vm v WHERE v.user_ = :user AND v.cloud = :cloud"),
		@NamedQuery(name = "usageByUser", query = "SELECT v.cloud, COUNT(v.runUuid) FROM Vm v WHERE v.user_ = :user AND v.state IN ('Running', 'running', 'On', 'on', 'active', 'Active') AND v.runUuid IS NOT NULL AND v.runUuid <> 'Unknown' GROUP BY v.cloud ORDER BY v.cloud"),
		@NamedQuery(name = "byRun", query = "SELECT v.cloud, v FROM Vm v WHERE v.runUuid = :run")
})

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

	@Attribute(required = false)
	private String runOwner;

	@Attribute(required = false)
	private String ip;

	@Attribute(required = false)
	private String name;

	@Attribute(required = false)
	private String nodeName;

	@Attribute(required = false)
	private String nodeInstanceId;

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

	public static List<Vm> list(User user) throws ConfigurationException, ValidationException {
		return list(user, null, null, null, null);
	}

	public static List<Vm> list(User user, Integer offset, Integer limit, String cloudServiceName, String runUuid)
			throws ConfigurationException, ValidationException {

		List<Vm> vms = null;
		EntityManager em = PersistenceUtil.createEntityManager();
		try {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Vm> critQuery = builder.createQuery(Vm.class);
			Root<Vm> rootQuery = critQuery.from(Vm.class);
			critQuery.select(rootQuery);
			Predicate where = viewListCommonQueryOptions(builder, rootQuery, user, cloudServiceName, runUuid);
			if (where != null){
				critQuery.where(where);
			}
			critQuery.orderBy(builder.desc(rootQuery.get("measurement")));
			TypedQuery<Vm> query = em.createQuery(critQuery);
			if (offset != null) {
				query.setFirstResult(offset);
			}
			if (limit != null) {
				query.setMaxResults(limit);
			}
			vms = query.getResultList();
		} finally {
			em.close();
		}
		return vms;
	}

	public static int listCount(User user, String cloudServiceName, String runUuid)
			throws ConfigurationException, ValidationException {
		int count = 0;
		EntityManager em = PersistenceUtil.createEntityManager();
		try {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Long> critQuery = builder.createQuery(Long.class);
			Root<Vm> rootQuery = critQuery.from(Vm.class);
			critQuery.select(builder.count(rootQuery));
			Predicate where = viewListCommonQueryOptions(builder, rootQuery, user, cloudServiceName, runUuid);
			if (where != null){
				critQuery.where(where);
			}
			TypedQuery<Long> query = em.createQuery(critQuery);
			count = (int)(long) query.getSingleResult();
		} finally {
			em.close();
		}
		return count;
	}

	private static Predicate andPredicate(CriteriaBuilder builder, Predicate currentPredicate, Predicate newPredicate){
		return (currentPredicate != null) ? builder.and(currentPredicate, newPredicate) : newPredicate;
	}

	private static Predicate viewListCommonQueryOptions(CriteriaBuilder builder, Root<Vm> rootQuery, User user,
			String cloudServiceName, String runUuid) {
		Predicate where = null;
		if (!user.isSuper()) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("user_"), user.getName()));
		}
		if (runUuid != null && !"".equals(runUuid)) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("runUuid"), runUuid));
		}
		if (cloudServiceName != null && !"".equals(cloudServiceName)) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("cloud"), cloudServiceName));
		}
		return where;
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
		Map<String, Vm> newVmsMap = toMapByInstanceId(newVms);
		int removed = 0;
		for (Vm vm : oldVmList) {
			String instanceId = vm.getInstanceId();
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
				setIp(m, v);
				setName(m, v);
				setRunUuid(m, v);
				setRunOwner(m, v);
				setNodeName(m, v);
				setNodeInstanceId(m, v);
				em.persist(v);
			} else {
				boolean merge = false;

				if (!v.getState().equals(old.getState())) {
					old.setState(v.getState());
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

	public String getRunOwner() {
		return runOwner;
	}

	public void setRunOwner(String runOwner) {
		this.runOwner = runOwner;
	}

	public String getIp() {
		return this.ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNodeName() {
		return this.nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getNodeInstanceId() {
		return this.nodeInstanceId;
	}

	public void setNodeInstanceId(String nodeInstanceId) {
		this.nodeInstanceId = nodeInstanceId;
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
