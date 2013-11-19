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

import java.util.HashMap;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
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

	private final static String CATEGORY_GENERAL = "General";
	private final static String CATEGORY_OVERALL_CAPACITY = "Overall Capacity";
	private final static String CATEGORY_SINGLE_VM_CAPACITY = "Single VM Capacity";
	private final static String CATEGORY_PRICING = "Pricing";
//	private final static String CATEGORY_OTHER = "Other";

	public final static String RESOURCE_URL_PREFIX = "servicecatalog/";

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
		return "servicecatalog/" + getCloud();
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
			if (scp != null) {
				scp.setCategory(dp.getCategory());
				scp.setDescription(dp.getDescription());
				scp.setInstructions(dp.getInstruction());
				scp.setType(dp.getType());
			} else {
				scp = new ServiceCatalogParameter(name, "",
						dp.getDescription());
				scp.setCategory(dp.getCategory());
				scp.setInstructions(dp.getInstruction());
				scp.setMandatory(false);
				scp.setReadonly(false);
				scp.setType(dp.getType());
				setParameter(scp);
			}
		}
	}

	public void clearParameters() {
		setParameters(new HashMap<String, ServiceCatalogParameter>());
	}

	/**
	 * Encoded names. '_' here corresponds to '.' in config file.
	 */
	public enum DefinedParameters {

		GENERAL_DESCRIPTION(
				"General description of the cloud service (including optional further links)",
				CATEGORY_GENERAL),

		SUPPORT_EMAIL("Support email adress for this cloud service",
				CATEGORY_GENERAL) {
			@Override
			public void validate(String value) {
				super.validate(value);
				isValidEmail(value);
			}
		},

		OVERALL_CPU("Overall available CPUs (cores)",
				CATEGORY_OVERALL_CAPACITY),

		OVERALL_RAM("Overall available RAM", CATEGORY_OVERALL_CAPACITY),

		OVERALL_STORAGE("Overall available storage", CATEGORY_OVERALL_CAPACITY),

		SINGLE_VM_MAX_CPU("Maximum number of CPUs (cores) available for a single VM",
				CATEGORY_SINGLE_VM_CAPACITY),

		SINGLE_VM_MAX_RAM("Maximum number of RAM (GB) available for a single VM",
				CATEGORY_SINGLE_VM_CAPACITY),

		PRICING_CPU_PER_HOUR("CPU cost per hour, in euro",
				CATEGORY_PRICING),

		PRICING_RAM_PER_HOUR("RAM (GB) cost per hour, in euro",
				CATEGORY_PRICING),

		PRICING_STORAGE_PER_HOUR("Storage (GB) cost per hour, in euro",
				CATEGORY_PRICING);
		
		private final String description;
		private final String category;
		private final String instructions;
		private final ParameterType type;
		private final boolean readonly;

		public String getInstructions() {
			return instructions;
		}

		private DefinedParameters(String description,
				String category) {
			this.description = description;
			this.category = category;
			this.instructions = "";
			this.type = ParameterType.String;
			this.readonly = false;
		}

		private DefinedParameters(String description,
				String category, boolean readonly) {
			this.description = description;
			this.category = category;
			this.instructions = "";
			this.type = ParameterType.String;
			this.readonly = readonly;
		}

		private DefinedParameters(String description,
				String category, String instructions) {
			this.description = description;
			this.category = category;
			this.instructions = instructions;
			this.type = ParameterType.String;
			this.readonly = false;
		}

		private DefinedParameters(String description,
				String category, ParameterType type) {
			this.description = description;
			this.category = category;
			this.instructions = "";
			this.type = type;
			this.readonly = false;
		}

		private DefinedParameters(String description,
				String category, String instructions,
				ParameterType type) {
			this.description = description;
			this.category = category;
			this.instructions = instructions;
			this.type = type;
			this.readonly = false;
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

		private static void isValidEmail(String s) {
			try {
				new InternetAddress(s);
			} catch (AddressException e) {
				throw new IllegalArgumentException("invalid email address: "
						+ e.getMessage());
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
