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

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;

import org.simpleframework.xml.Attribute;

import com.sixsq.slipstream.exceptions.NotImplementedException;
import com.sixsq.slipstream.exceptions.ValidationException;

/**
 * Unit test:
 * 
 * @see ServiceCatalogTest
 * 
 */
@SuppressWarnings("serial")
@Entity
@NamedQueries({
		@NamedQuery(name = "byCloud", query = "SELECT sc FROM ServiceCatalog sc WHERE sc.cloud = :cloud"),
		@NamedQuery(name = "all", query = "SELECT sc FROM ServiceCatalog sc") })
public class ServiceCatalog extends
		Parameterized<ServiceCatalog, ServiceCatalogParameter> {

	private final static String CATEGORY_OVERALL_CAPACITY = "Overall capacity";
	private final static String CATEGORY_SINGLE_VM_CAPACITY = "Single VM capacity";
	private final static String CATEGORY_PRICE = "Price";
	private final static String CATEGORY_LOCATION = "Locations";
	private final static String CATEGORY_SUPPLIERS_CATALOG = "Suppliers catalogue";

	public final static String RESOURCE_URL_PREFIX = "service_catalog/";

	@Attribute
	@Id
	private String resourceUri;

	@Attribute
	private String cloud;

	@SuppressWarnings("unused")
	private ServiceCatalog() {
	}

	public ServiceCatalog(String cloud) {
		setName(cloud);
	}

	public String getCloud() {
		return cloud;
	}

	@Override
	public void validate() throws ValidationException {
		validateParameters();
	}

	@SuppressWarnings("unchecked")
	public static List<ServiceCatalog> list(String cloud) {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("byCloud");
		q.setParameter("cloud", cloud);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public static List<ServiceCatalog> listall() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("all");
		return q.getResultList();
	}

	@Override
	public void setContainer(ServiceCatalogParameter parameter) {
		parameter.setContainer(this);
	}

	@Override
	public String getResourceUri() {
		return constructResourceUri(getCloud());
	}

	@Override
	public String getName() {
		return getCloud();
	}

	@Override
	public void setName(String cloud) {
		this.cloud = cloud;
		this.resourceUri = constructResourceUri(cloud);
	}

	public static String constructResourceUri(String name) {
		return RESOURCE_URL_PREFIX + name;
	}

	public static ServiceCatalog loadByCloud(String cloud) {
		return load(constructResourceUri(cloud));
	}

	public static ServiceCatalog load(String resourceUrl) {
		EntityManager em = PersistenceUtil.createEntityManager();
		ServiceCatalog sc = em.find(ServiceCatalog.class, resourceUrl);
		em.close();
		return sc;
	}

	public static ServiceCatalog load() {
		throw new NotImplementedException();
	}

	@Override
	public ServiceCatalog store() {
		return (ServiceCatalog) super.store();
	}

	public void populateDefinedParameters() throws ValidationException {
		for (DefinedParameters dp : DefinedParameters.values()) {
			String name = cloud + "." + dp.getName();
			ServiceCatalogParameter scp = getParameter(name);
			if (scp == null) {
				scp = new ServiceCatalogParameter(name, "", dp.getDescription());
			}
			scp.setCategory(dp.getCategory());
			scp.setDescription(dp.getDescription());
			scp.setInstructions(dp.getInstruction());
			scp.setType(dp.getType());
			scp.setReadonly(true);
			scp.setMandatory(true);
			setParameter(scp);
		}
	}

	public void clearParameters() {
		for (ServiceCatalogParameter p : getParameters().values()) {
			p.setContainer(null);
		}
	}

	/**
	 * Encoded names. '_' here corresponds to '.' in config file.
	 */
	public enum DefinedParameters {

		// Overall capacity

		OVERALL_CAPACITY_CPU(
				"The number of CPU cores (currently) available within (the relevant part of) the supplier’s IaaS environment",
				CATEGORY_OVERALL_CAPACITY, "Value: an integer, and possibly approximate, number, e.g. 1,000; Explanation: to give an indication of the scale of the environment available for use"),

		OVERALL_CAPACITY_RAM(
				"Nature: the amount of random-access memory in total; Value: expressed in relevant terms, e.g. 10 TB; Explanation: the amount of memory available across the installation as a whole. See below for what is available on any one system",
				CATEGORY_OVERALL_CAPACITY),

		OVERALL_CAPACITY_STORAGE(
				"Nature: the amount of persistent storage (e.g. SSD, disk, tape) available within that supplier’s environment; Value: expressed in relevant terms, e.g. 10 PB; Explanation: possibly multiple values, e.g. per technology type",
				CATEGORY_OVERALL_CAPACITY),

		// Single vm capacity

		SINGLE_VM_MIN_CPU(
				"Nature: the minimum number of CPU cores with which this supplier’s VMs can be configured; Value: an integer number, e.g. 1; Explanation: to give an indication of the minimum configurable environment",
				CATEGORY_SINGLE_VM_CAPACITY),

		SINGLE_VM_MAX_CPU(
				"Nature: the maximum number of CPU cores with which this supplier’s  VMs can be configured; Value: an integer number, e.g. 8; Explanation: to give an indication of the maximum configurable environment",
				CATEGORY_SINGLE_VM_CAPACITY),

		SINGLE_VM_MIN_RAM(
				"Nature: the minimum amount of random-access memory (currently) available within VMs; Value: expressed in relevant terms, e.g. 128 GB; Explanation: the amount of memory available to any one VM within the supplier’s IaaS environment",
				CATEGORY_SINGLE_VM_CAPACITY),

		SINGLE_VM_MAX_RAM(
				"Nature: the maximum amount of random-access memory (currently) available within VMs; Value: expressed in relevant terms, e.g. 128 GB; Explanation: the amount of memory available to any one VM within the supplier’s IaaS environment",
				CATEGORY_SINGLE_VM_CAPACITY),

		SINGLE_VM_STORAGE_VOLATILE(
				"Nature: the amount of volatile storage available locally to that VM; Value: expressed in relevant terms, e.g. 500 GB; Explanation: the amount of “scratch” space, which could be used, e.g. to extend the random access memory of a VM. Local disk space is typically slower than ram but faster than persistent storage space",
				CATEGORY_SINGLE_VM_CAPACITY),

		SINGLE_VM_STORAGE_PERSISTENT(
				"Nature: the amount of persistent storage (e.g. SSD, disk, tape) available to that VM; Storage access method (e.g. local or network); Storage type: block device or network mount; Value: expressed in relevant terms, e.g. 10 TB per drive/block device; Resilience level or equivalent eg. RAID6, RAID5 etc. Explanation: possibly multiple values, e.g. per technology type. This presumes that storage is associated with a particular VM, i.e. it is locally attached or via a restricted network. Otherwise, it could be up to the total figure, as above",
				CATEGORY_SINGLE_VM_CAPACITY),

		// Price

		PRICE_CHARGING_UNIT(
				"Nature: the unit used for charging; Value: the pricing unit, e.g. GHz, portion of CPU chip, etc.; Explanation: this could vary per supplier, as there is no standard unit. Work from the ODCA or Deutsche Boerse could be used to derive such a standard, at least for comparative purposes, in the future",
				CATEGORY_PRICE),

		PRICE_CHARGING_PERIOD(
				"Nature: the period used for charging; Value: the pricing period, e.g. hour, month; Explanation: this could vary per resource, e.g. CPU per hour, storage per month",
				CATEGORY_PRICE),

		PRICE_CPU_PER_HOUR(
				"Nature: the price for use of a unit of processing per period, e.g. hour; Value: the price in euros, e.g. €0.05",
				CATEGORY_PRICE),

		PRICE_RAM_PER_HOUR(
				"Nature: the price for use of a unit (e.g. 1 GB) of memory per hour; Value: the price in euros, e.g. €0.05",
				CATEGORY_PRICE),

		PRICE_STORAGE_PER_HOUR(
				"Nature: the price for use of a unit (e.g. 1 GB) of storage per hour; Value: the price in euros, e.g. €0.0005; Explanation: note that is possible that storage is either associated with a particular VM or as a generally-available resource",
				CATEGORY_PRICE),

		PRICE_IO(
				"Nature: the price for transmitting a unit (e.g. 1 GB) in or out of the environment; Value: the price in euros, e.g. €0.30",
				CATEGORY_PRICE),

		// Location

		LOCATION(
				"Nature: geographical location of relevant data centre(s); Value: ISO-standard country code and name for cloud location and operational company location, e.g. NL The Netherlands; Explanation: currently, data protection legislation differs per country",
				CATEGORY_LOCATION),

		// Suppliers catalog

		SUPPLIERS_CATALOG(
				"Nature: URL of web site with further details; Value: e.g. http://example.com",
				CATEGORY_SUPPLIERS_CATALOG);

		private final String description;
		private final String category;
		private final String instructions;
		private final ParameterType type;
		private final boolean readonly;

		public String getInstructions() {
			return instructions;
		}

		private DefinedParameters(String description, String category) {
			this.description = description;
			this.category = category;
			this.instructions = "";
			this.type = ParameterType.String;
			this.readonly = true;
		}

		private DefinedParameters(String description, String category,
				String instructions) {
			this.description = description;
			this.category = category;
			this.instructions = instructions;
			this.type = ParameterType.String;
			this.readonly = true;
		}

		public String getInstruction() {
			return instructions;
		}

		public String getDescription() {
			return description;
		}

		public String getCategory() {
			return category;
		}

		public void validate(String value) {
			if (isReadonly()) {
				return;
			}

			if (getType() == ParameterType.Boolean && value == null) {
				return;
			}
			if (value == null || "".equals(value)) {
				throw new IllegalArgumentException(
						"value cannot be empty or null");
			}
		}

		public ParameterType getType() {
			return type;
		}

		public boolean isReadonly() {
			return readonly;
		}

		/**
		 * Convert the enum name into parameter name where word separators are
		 * converted from _ to . and lower cased.
		 */
		public String getName() {
			return name().replace("_", ".").toLowerCase();
		}
	}

}
