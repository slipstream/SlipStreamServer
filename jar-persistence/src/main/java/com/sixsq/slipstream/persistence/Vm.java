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

import javax.persistence.Column;
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
import com.sixsq.slipstream.vm.VmsQueryParameters;

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
		@NamedQuery(name = "usageByUser", query = "SELECT v.cloud, COUNT(v.runUuid) FROM Vm v WHERE v.user_ = :user AND v.isUsable = 1 AND v.runUuid IS NOT NULL AND v.runUuid <> 'Unknown' GROUP BY v.cloud ORDER BY v.cloud"),
		@NamedQuery(name = "byUser", query = "SELECT v.measurement, v.runUuid, v.runOwner, v.cloud, v.isUsable FROM Vm v WHERE v.user_ = :user"),
		@NamedQuery(name = "byRun", query = "SELECT v.cloud, v FROM Vm v WHERE v.runUuid = :run")
})

public class Vm {

	public final static String RESOURCE_URL_PREFIX = "vms/";
	public static final int PENDING_TIMEOUT = 15000;

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

	@Column(nullable = true)
	@Attribute(required = false)
	private Boolean isUsable;

	@Attribute(required = false)
	private Integer cpu;

	@Attribute(required = false)
	private Float ram;

	@Attribute(required = false)
	private Float disk;

	@Attribute(required = false)
	private String instanceType;

	@SuppressWarnings("unused")
	private Vm() {
	}

	public Vm(String instanceid, String cloud, String state, String user, boolean isUsable, String cpu, String ram, String disk, String instanceType) {
		this.instanceId = instanceid;
		this.cloud = cloud;
		this.state = state;
		this.user_ = user;
		this.isUsable = isUsable;
		this.cpu = (cpu == null) ? null : Integer.valueOf(cpu);
		this.ram = (ram == null) ? null : Float.valueOf(ram);
		this.disk = (disk == null) ? null : Float.valueOf(disk);
		this.instanceType = instanceType;
		measurement = new Date();
	}

	public Vm(String instanceid, String cloud, String state, String user, boolean isUsable) {
		this.instanceId = instanceid;
		this.cloud = cloud;
		this.state = state;
		this.user_ = user;
		this.isUsable = isUsable;
		measurement = new Date();
	}

	public static List<Vm> list(User user) throws ConfigurationException, ValidationException {
		return list(new VmsQueryParameters(user, null, null, null, null, null, null));
	}

	public static List<Vm> list(VmsQueryParameters parameters)
			throws ConfigurationException, ValidationException {

		List<Vm> vms = null;
		EntityManager em = PersistenceUtil.createEntityManager();
		try {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Vm> critQuery = builder.createQuery(Vm.class);
			Root<Vm> rootQuery = critQuery.from(Vm.class);
			critQuery.select(rootQuery);
			Predicate where = viewListCommonQueryOptions(builder, rootQuery, parameters);
			if (where != null){
				critQuery.where(where);
			}
			critQuery.orderBy(builder.desc(rootQuery.get("measurement")));
			TypedQuery<Vm> query = em.createQuery(critQuery);
			if (parameters.offset != null) {
				query.setFirstResult(parameters.offset);
			}
			if (parameters.limit != null) {
				query.setMaxResults(parameters.limit);
			}
			vms = query.getResultList();
		} finally {
			em.close();
		}
		return vms;
	}

	public static int listCount(VmsQueryParameters parameters) throws ConfigurationException, ValidationException {
		int count = 0;
		EntityManager em = PersistenceUtil.createEntityManager();
		try {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Long> critQuery = builder.createQuery(Long.class);
			Root<Vm> rootQuery = critQuery.from(Vm.class);
			critQuery.select(builder.count(rootQuery));
			Predicate where = viewListCommonQueryOptions(builder, rootQuery, parameters);
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

	private static Predicate viewListCommonQueryOptions(CriteriaBuilder builder, Root<Vm> rootQuery,
			VmsQueryParameters parameters) {

		Predicate where = null;
		if (!parameters.user.isSuper()) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("user_"), parameters.user.getName()));
		}
		if (parameters.user.isSuper() && parameters.userFilter != null) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("user_"), parameters.userFilter));
		}
		if (parameters.runUuid != null && !"".equals(parameters.runUuid)) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("runUuid"), parameters.runUuid));
		}
		if (parameters.runOwner != null && !"".equals(parameters.runOwner)) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("runOwner"), parameters.runOwner));
		}
		if (parameters.cloud != null && !"".equals(parameters.cloud)) {
			where = andPredicate(builder, where, builder.equal(rootQuery.get("cloud"), parameters.cloud));
		}
		return where;
	}

	private static CloudUsage addCloudIntoUsage(Map<String, CloudUsage> usages, String cloud) {
		CloudUsage usage;

		if (!usages.containsKey(cloud)) {
			usage = new CloudUsage(cloud);
			usages.put(cloud, usage);
		} else {
			usage = usages.get(cloud);
		}
		return usage;
	}

	public static Map<String, CloudUsage> usage(String user) {
		List<?> vms;
		EntityManager em = PersistenceUtil.createEntityManager();
		try {
			vms = em.createNamedQuery("byUser").setParameter("user", user).getResultList();
		} finally {
			em.close();
		}

		Map<String, CloudUsage> usages = new HashMap<>();
		for (Object vm : vms) {
			Date measurement = (Date) ((Object[]) vm)[0];
			String runUuid = (String) ((Object[]) vm)[1];
			String runOwner = (String) ((Object[]) vm)[2];
			String cloud = (String) ((Object[]) vm)[3];
			Boolean isUsable = (Boolean) ((Object[]) vm)[4];

			if (user.equals(runOwner)) {
				if (isUsable) {
					addCloudIntoUsage(usages, cloud).incrementUserVmUsage();
				} else {
					addCloudIntoUsage(usages, cloud).incrementUserInactiveVmUsage();
				}
			} else if (runUuid != null) {
				addCloudIntoUsage(usages, cloud).incrementOthersVmUsage();
			} else if (((new Date()).getTime() - measurement.getTime()) < PENDING_TIMEOUT) {
				addCloudIntoUsage(usages, cloud).incrementPendingVmUsage();
			} else {
				addCloudIntoUsage(usages, cloud).incrementUnknownVmUsage();
			}
		}

		return usages;
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

	public void setState(String state) {
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

	public boolean getIsUsable() {
		return (this.isUsable == null) ? false : this.isUsable;
	}

	public void setIsUsable(boolean isUsable) {
		this.isUsable = isUsable;
	}

	public Integer getCpu() {
		return this.cpu;
	}

	public void setCpu(Integer cpu) {
		this.cpu = cpu;
	}

	public void setCpu(String cpu) {
		this.cpu = (cpu == null) ? null : Integer.valueOf(cpu);
	}

	public Float getRam() {
		return this.ram;
	}

	public void setRam(Float ram) {
		this.ram = ram;
	}

	public void setRam(String ram) {
		this.ram = (ram == null) ? null : Float.valueOf(ram);
	}

	public Float getDisk() {
		return this.disk;
	}

	public void setDisk(Float disk) {
		this.disk = disk;
	}

	public void setDisk(String disk) {
		this.disk = (disk == null) ? null : Float.valueOf(disk);
	}

	public String getInstanceType() {
		return this.instanceType;
	}

	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
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
