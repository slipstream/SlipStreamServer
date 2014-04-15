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
			@NamedQuery(name = "usageByUser", query = "SELECT v.cloud, COUNT(v.runUuid) FROM Vm v WHERE v.user_ = :user AND v.state IN ('Running', 'running', 'On', 'on', 'active', 'Active') AND v.runUuid IS NOT NULL AND v.runUuid <> 'Unknown' GROUP BY v.cloud ORDER BY v.cloud"),
			@NamedQuery(name = "removeByUser", query = "DELETE Vm WHERE user_ = :user AND cloud = :cloud") })
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
	
	@Attribute
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
		Query q = em.createNamedQuery("removeByUser");
		q.setParameter("user", user);
		q.setParameter("cloud", cloud);
		int removed = q.executeUpdate();
		for(Vm v : newVms) {
			em.persist(v);
		}
		transaction.commit();
		em.close();
		return removed;
	}

	public static Map<String, Integer> usage(String user) {
		EntityManager em = PersistenceUtil.createEntityManager();
		
		List<?> res = em.createNamedQuery("usageByUser")
			.setParameter("user", user)
			.getResultList();
		em.close();
		
		Map<String, Integer> usageData = new HashMap<String, Integer>(res.size());
		for (Object object : res) {
			usageData.put((String)((Object[])object)[0], 
				      ((Long)((Object[])object)[1]).intValue());
		}
		
		return usageData;
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
	
	public static Map<String, Vm> toMap(List<Vm> vms) {
		Map<String, Vm> map = new HashMap<String, Vm>();
		for(Vm v : vms) {
			map.put(v.getInstanceId(), v);
		}
		return map;
	}

}
