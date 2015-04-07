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
	private String runOwner;
	private String name;
	private String nodeName;
	private String nodeInstanceId;

	@Transient
	volatile private RuntimeParameter vmstateRuntimeParameter = null;

	private String vmstateRuntimeParameterUri;

	@Transient
	volatile private RuntimeParameter hostnameRuntimeParameter = null;

	private String hostnameRuntimeParameterUri;


	public static VmRuntimeParameterMapping find(String cloud, String instanceId) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("getByCloudAndInstanceId");
		q.setParameter("cloud", cloud);
		q.setParameter("instanceid", instanceId);

		@SuppressWarnings("unchecked")
		List<VmRuntimeParameterMapping> list = q.getResultList();

		em.close();
		if (list.size() > 1) {
			Logger.warning("found more than one cloud/instanceid tuple: " + cloud + " / " + instanceId);
		}
		return list.isEmpty() ? null : list.get(0);
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

	private static RuntimeParameter getRuntimeParameter(String runUuid, String nodeInstanceName, String parameterName) {
		String key = RuntimeParameter.constructParamName(nodeInstanceName, parameterName);
		return RuntimeParameter.loadFromUuidAndKey(runUuid, key);
	}

	private static String getRuntimeParameterValue(RuntimeParameter parameter) {
		return (parameter != null)? parameter.getValue() : null;
	}

	private static String getRuntimeParameterValue(String runUuid, String nodeInstanceName, String parameterName) {
		return getRuntimeParameterValue(getRuntimeParameter(runUuid, nodeInstanceName, parameterName));
	}

	public static void insertVmInstanceMapping(RuntimeParameter instanceIdParameter) {
		String nodeInstanceName = instanceIdParameter.getNodeName();
		Run run = instanceIdParameter.getContainer();
		String runUuid = run.getUuid();
		String runOwner = run.getUser();
		String instanceId = getRuntimeParameterValue(instanceIdParameter);
		String name = instanceIdParameter.getGroup();

		String cloud = getRuntimeParameterValue(runUuid, nodeInstanceName, RuntimeParameter.CLOUD_SERVICE_NAME);
		String nodeName = getRuntimeParameterValue(runUuid, nodeInstanceName, RuntimeParameter.NODE_NAME_KEY);
		String nodeInstanceid = getRuntimeParameterValue(runUuid, nodeInstanceName, RuntimeParameter.NODE_ID_KEY);

		RuntimeParameter vmstate = getRuntimeParameter(runUuid, nodeInstanceName, RuntimeParameter.STATE_VM_KEY);
		RuntimeParameter hostname = getRuntimeParameter(runUuid, nodeInstanceName, RuntimeParameter.HOSTNAME_KEY);

		VmRuntimeParameterMapping m = new VmRuntimeParameterMapping(instanceId, cloud, runOwner, name, nodeName,
				nodeInstanceid, vmstate, hostname);
		m.store();
	}

	public VmRuntimeParameterMapping() {

	}

	public VmRuntimeParameterMapping(String instanceId, String cloud, String runOwner, String name, String nodeName,
			String nodeInstanceId,
			RuntimeParameter vmstateRuntimeParameter,
			RuntimeParameter hostnameRuntimeParameter) {
		this.instanceId = instanceId;
		this.cloud = cloud;
		this.name = name;
		this.nodeName = nodeName;
		this.nodeInstanceId = nodeInstanceId;
		this.runUuid = vmstateRuntimeParameter.getContainer().getUuid();
		this.runOwner = runOwner;
		this.vmstateRuntimeParameter = vmstateRuntimeParameter;
		this.vmstateRuntimeParameterUri = vmstateRuntimeParameter.getResourceUri();
		this.hostnameRuntimeParameter = hostnameRuntimeParameter;
		this.hostnameRuntimeParameterUri = hostnameRuntimeParameter.getResourceUri();
	}

	public String getInstanceId() {
		return instanceId;
	}

	public String getCloud() {
		return cloud;
	}

	public String getName() {
		return this.name;
	}

	public String getNodeName() {
		return this.nodeName;
	}

	public String getNodeInstanceId() {
		return this.nodeInstanceId;
	}

	public String getRunUuid() {
		return runUuid;
	}

	public String getRunOwner() {
		return runOwner;
	}

	public RuntimeParameter getVmstateRuntimeParameter() {
		if(vmstateRuntimeParameter == null) {
			vmstateRuntimeParameter = RuntimeParameter.load(vmstateRuntimeParameterUri);
		}
		return vmstateRuntimeParameter;
	}

	public RuntimeParameter getHostnameRuntimeParameter() {
		if(hostnameRuntimeParameter == null) {
			hostnameRuntimeParameter = RuntimeParameter.load(hostnameRuntimeParameterUri);
		}
		return hostnameRuntimeParameter;
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
