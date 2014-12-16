package com.sixsq.slipstream.persistence;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2014 SixSq Sarl (sixsq.com)
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
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.sixsq.slipstream.exceptions.SlipStreamDatabaseException;
import com.sixsq.slipstream.util.Logger;

@SuppressWarnings("serial")
@Entity
@NamedQueries({
		@NamedQuery(name = "getAll", query = "SELECT m FROM VmRuntimeParameterMapping m"),
		@NamedQuery(name = "getByCloudAndInstanceId", query = "SELECT m FROM VmRuntimeParameterMapping m WHERE m.instanceId = :instanceid AND m.cloud = :cloud"),
		@NamedQuery(name = "getMappingsByRun", query = "SELECT m FROM VmRuntimeParameterMapping m WHERE m.runUuid = :runuuid") })
@Table(indexes = { @Index(name = "instanceId_ix", columnList = "instanceId"),
		@Index(name = "cloud_ix", columnList = "cloud"), @Index(name = "runUuid_ix", columnList = "runUuid") })
public class VmRuntimeParameterMapping implements Serializable {

	@Id
	@GeneratedValue
	Long id;

	private String instanceId;
	private String cloud;
	private String runUuid;

	@Transient
	volatile private RuntimeParameter runtimeParameter = null;

	private String runtimeParameterUri;

	public static VmRuntimeParameterMapping findRuntimeParameter(String cloud, String instanceId) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("getByCloudAndInstanceId");
		q.setParameter("cloud", cloud);
		q.setParameter("instanceid", instanceId);

		@SuppressWarnings("unchecked")
		List<VmRuntimeParameterMapping> list = q.getResultList();

		em.close();
		boolean isEmpty = list.isEmpty();
		if (!isEmpty) {
			Logger.warning("found more than one cloud/instanceid tuple: " + cloud + " / " + instanceId);
		}
		return isEmpty ? null : list.get(0);
	}

	public static List<VmRuntimeParameterMapping> getMappings(String uuid) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("getMappingsByRun");
		q.setParameter("runuuid", uuid);

		@SuppressWarnings("unchecked")
		List<VmRuntimeParameterMapping> list = q.getResultList();

		em.close();
		return list;
	}

	public static List<VmRuntimeParameterMapping> getMappings() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("getAll");

		@SuppressWarnings("unchecked")
		List<VmRuntimeParameterMapping> list = q.getResultList();

		em.close();
		return list;
	}

	public static void insertVmInstanceMapping(RuntimeParameter instanceId) {
		String nodeInstanceName = instanceId.getNodeName();
		String cloudParameterName = RuntimeParameter.constructParamName(nodeInstanceName,
				RuntimeParameter.CLOUD_SERVICE_NAME);
		RuntimeParameter cloudParameter = RuntimeParameter.loadFromUuidAndKey(instanceId.getContainer().getUuid(),
				cloudParameterName);
		VmRuntimeParameterMapping m = new VmRuntimeParameterMapping(instanceId.getValue(), cloudParameter.getValue(),
				instanceId);
		m.store();
	}

	public VmRuntimeParameterMapping() {

	}

	public VmRuntimeParameterMapping(String instanceId, String cloud, RuntimeParameter runtimeParameter) {
		this.instanceId = instanceId;
		this.cloud = cloud;
		this.runtimeParameter = runtimeParameter;
		this.runtimeParameterUri = runtimeParameter.getResourceUri();
		this.runUuid = runtimeParameter.getContainer().getUuid();
	}

	public String getCloud() {
		return cloud;
	}

	public RuntimeParameter getRuntimeParameter() {
		if(runtimeParameter == null) {
			runtimeParameter = RuntimeParameter.load(runtimeParameterUri);
		}
		return runtimeParameter;
	}

	public String getRunUuid() {
		return runUuid;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public static VmRuntimeParameterMapping load(Long id) {
		EntityManager em = PersistenceUtil.createEntityManager();
		VmRuntimeParameterMapping m = em.find(VmRuntimeParameterMapping.class, id);
		em.close();
		return m;
	}

	public VmRuntimeParameterMapping store() throws SlipStreamDatabaseException {
		VmRuntimeParameterMapping obj = null;
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = null;
		try {
			transaction = em.getTransaction();
			transaction.begin();
			obj = em.merge(this);
			transaction.commit();
		} catch (PersistenceException e) {
			transaction.rollback();
			throw new SlipStreamDatabaseException(e.getMessage());
		} finally {
			em.close();
		}
		return obj;
	}

	public void remove() {
		EntityManager em = PersistenceUtil.createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		VmRuntimeParameterMapping obj = em.merge(this);
		em.remove(obj);
		transaction.commit();
		em.close();
	}
}
