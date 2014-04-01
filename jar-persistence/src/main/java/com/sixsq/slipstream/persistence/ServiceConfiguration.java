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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;

import com.sixsq.slipstream.exceptions.NotImplementedException;

@SuppressWarnings("serial")
@Entity
@NamedQueries({ @NamedQuery(name = "latestConfiguration", query = "SELECT c FROM ServiceConfiguration c WHERE c.id = (SELECT MAX(c.id) FROM ServiceConfiguration c)") })
public class ServiceConfiguration extends
		Parameterized<ServiceConfiguration, ServiceConfigurationParameter> {

	public final static String RESOURCE_URI_PREFIX = "configuration/";

	public enum ParameterCategory {
		SlipStream_Support, SlipStream_Basics, SlipStream_Advanced
	}

	/**
	 * Encoded names. '_' here corresponds to '.' in config file.
	 */
	public enum RequiredParameters {

		SLIPSTREAM_BASE_URL("Default URL and port for the SlipStream RESTlet",
				ParameterCategory.SlipStream_Basics),

		SLIPSTREAM_UPDATE_CLIENTURL(
				"Endpoint of the SlipStream client tarball",
				ParameterCategory.SlipStream_Advanced),

		SLIPSTREAM_UPDATE_CLIENTBOOTSTRAPURL(
				"Endpoint of the SlipStream client bootstrap script",
				ParameterCategory.SlipStream_Advanced),

		CLOUD_CONNECTOR_CLASS(
				"Cloud connector java class name(s) (comma separated for multi-cloud configuration)",
				ParameterCategory.SlipStream_Basics, ParameterType.Text),

		CLOUD_CONNECTOR_LIBRARY_LIBCLOUD_URL(
				"URL to fetch libcloud library from",
				ParameterCategory.SlipStream_Advanced,
				"URL should point to a valid gzipped tarball."),

		CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY(
				"Path to the SSH public key to put in the orchestrator",
				ParameterCategory.SlipStream_Advanced),

		CLOUD_CONNECTOR_ORCHESTRATOR_PRIVATESSHKEY(
				"Path to the SSH private key used to connect to the orchestrator (used only for some Clouds)",
				ParameterCategory.SlipStream_Advanced),

		SLIPSTREAM_VERSION("Installed SlipStream version",
				ParameterCategory.SlipStream_Advanced, true),

		SLIPSTREAM_REPORTS_LOCATION(
				"Location where the deployments and build reports are saved",
				ParameterCategory.SlipStream_Advanced),

		SLIPSTREAM_REGISTRATION_EMAIL(
				"Email address for account approvals, etc.",
				ParameterCategory.SlipStream_Support,
				"<h1>email address</h1> to use for registration") {
			@Override
			public void validate(String value) {
				super.validate(value);
				isValidEmail(value);
			}
		},

		SLIPSTREAM_MAIL_HOST("Host for SMTP server for email notifications.",
				ParameterCategory.SlipStream_Support),

		SLIPSTREAM_MAIL_PORT(
				"Port on SMTP server (defaults to standard ports).",
				ParameterCategory.SlipStream_Support) {
			@Override
			public void validate(String value) {
				super.validate(value);
				isValidPort(value);
			}
		},

		SLIPSTREAM_MAIL_USERNAME(
				"Username for SMTP server.",
				ParameterCategory.SlipStream_Support,
				"Username of the mail server account you wan to use to send registration emails."),

		SLIPSTREAM_MAIL_PASSWORD("Password for SMTP server.",
				ParameterCategory.SlipStream_Support, ParameterType.Password),

		SLIPSTREAM_MAIL_SSL("Use SSL for SMTP server.",
				ParameterCategory.SlipStream_Support, ParameterType.Boolean),

		SLIPSTREAM_MAIL_DEBUG("Debug mail sending.",
				ParameterCategory.SlipStream_Support, ParameterType.Boolean),

		SLIPSTREAM_SUPPORT_EMAIL(
				"Email address for SlipStream support requests",
				ParameterCategory.SlipStream_Support) {
			@Override
			public void validate(String value) {
				super.validate(value);
				isValidEmail(value);
			}
		},

		SLIPSTREAM_REGISTRATION_ENABLE(
				"Allow user self registration. If checked, user will be able to create accounts themselves.",
				ParameterCategory.SlipStream_Basics, ParameterType.Boolean),

		SLIPSTREAM_SERVICE_CATALOG_ENABLE(
				"Enable service catalog feature.",
				ParameterCategory.SlipStream_Advanced, ParameterType.Boolean),

		SLIPSTREAM_METERING_HOSTNAME(
				"Metering server full hostname, including protocol, hostname/ip and port (e.g. http://localhost:2005).",
				ParameterCategory.SlipStream_Advanced),

		SLIPSTREAM_METERING_ENABLE("Metering enabled",
				ParameterCategory.SlipStream_Advanced, ParameterType.Boolean),

		SLIPSTREAM_QUOTA_ENABLE("Quota enforcement enabled",
				ParameterCategory.SlipStream_Advanced, ParameterType.Boolean);

		private final String description;
		private final ParameterCategory category;
		private final String instructions;
		private final ParameterType type;
		private final boolean readonly;

		public String getInstructions() {
			return instructions;
		}

		private RequiredParameters(String description,
				ParameterCategory category) {
			this.description = description;
			this.category = category;
			this.instructions = "";
			this.type = ParameterType.String;
			this.readonly = false;
		}

		private RequiredParameters(String description,
				ParameterCategory category, boolean readonly) {
			this.description = description;
			this.category = category;
			this.instructions = "";
			this.type = ParameterType.String;
			this.readonly = readonly;
		}

		private RequiredParameters(String description,
				ParameterCategory category, String instructions) {
			this.description = description;
			this.category = category;
			this.instructions = instructions;
			this.type = ParameterType.String;
			this.readonly = false;
		}

		private RequiredParameters(String description,
				ParameterCategory category, ParameterType type) {
			this.description = description;
			this.category = category;
			this.instructions = "";
			this.type = type;
			this.readonly = false;
		}

		private RequiredParameters(String description,
				ParameterCategory category, String instructions,
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

		public ParameterCategory getCategory() {
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

		private static void isValidPort(String s) {
			try {
				int port = Integer.parseInt(s);
				if (port < 1 || port > 65535) {
					throw new IllegalArgumentException("invalid port number("
							+ port + ")");
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(e.getMessage());
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

	@Id
	String id;

	public ServiceConfiguration() {
		setId();
	}

	public Map<String, ServiceConfigurationParameter> getParameters() {
		return parameters;
	}

	public void setParameters(
			Map<String, ServiceConfigurationParameter> parameters) {
		if (parameters == null) {
			parameters = new HashMap<String, ServiceConfigurationParameter>();
		}
		for (ServiceConfigurationParameter p : parameters.values()) {
			setParameter(p);
		}
	}

	public ServiceConfigurationParameter getParameter(String name) {
		return getParameters().get(name);
	}

	public void setParameter(ServiceConfigurationParameter parameter) {

		validateParameter(parameter);

		Map<String, ServiceConfigurationParameter> parameters = getParameters();

		parameter.setContainer(this);
		parameters.put(parameter.getName(), parameter);
	}

	public Parameter<ServiceConfiguration> getParameter(String name,
			ParameterCategory category) {
		Parameter<ServiceConfiguration> parameter = getParameter(name);
		if (parameter != null && parameter.getCategory().equals(category)) {
			return parameter;
		} else {
			return null;
		}
	}

	public Map<String, Parameter<ServiceConfiguration>> getParameters(
			ParameterCategory category) {
		Map<String, Parameter<ServiceConfiguration>> filteredParameters = new HashMap<String, Parameter<ServiceConfiguration>>();
		for (Parameter<ServiceConfiguration> parameter : getParameters()
				.values()) {
			if (parameter.getCategory().equals(category)) {
				filteredParameters.put(parameter.getName(), parameter);
			}
		}

		return filteredParameters;
	}

	public Collection<ServiceConfigurationParameter> getParameterList() {
		return getParameters().values();
	}

	public String getId() {
		return id;
	}

	public void setId() {
		id = RESOURCE_URI_PREFIX + String.valueOf(System.currentTimeMillis());
	}

	public void setContainer(ServiceConfigurationParameter parameter) {
		parameter.setContainer(this);
	}

	private void validateParameter(ServiceConfigurationParameter parameter) {
	}

	public void validate() {

		// Check that all of the required parameters are present.
		for (RequiredParameters p : RequiredParameters.values()) {
			if (getParameter(p.getName()) == null) {
				throw new IllegalArgumentException(
						"missing required system configuration parameter: "
								+ p.name());
			}
		}

	}

	public static ServiceConfiguration load() {
		EntityManager em = PersistenceUtil.createEntityManager();
		Query q = em.createNamedQuery("latestConfiguration");
		return (ServiceConfiguration) q.getSingleResult();
	}

	public ServiceConfiguration store() {
		validate();
		setId();
		return (ServiceConfiguration) super.store();
	}

	@Override
	public String getResourceUri() {
		return id;
	}

	@Override
	public String getName() {
		return "configuration";
	}

	@Override
	public void setName(String name) {
	}

	@Override
	public void remove() {
		throw (new NotImplementedException());
	}

}
