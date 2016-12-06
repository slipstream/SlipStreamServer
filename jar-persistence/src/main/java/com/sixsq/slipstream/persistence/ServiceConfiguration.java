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

import com.sixsq.slipstream.es.CljElasticsearchHelper;
import com.sixsq.slipstream.exceptions.NotImplementedException;
import java.util.logging.Logger;
import org.simpleframework.xml.ElementMap;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import clojure.lang.IFn;

@SuppressWarnings("serial")
public class ServiceConfiguration extends
		Parameterized<ServiceConfiguration, ServiceConfigurationParameter> {

	private static Logger logger = Logger.getLogger(CljElasticsearchHelper.class.getName());

	private final static String RESOURCE_URI = "configuration/slipstream";
    public final static String CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY = "cloud.connector.orchestrator.publicsshkey";
    public final static String CLOUD_CONNECTOR_ORCHESTRATOR_PRIVATESSHKEY = "cloud.connector.orchestrator.privatesshkey";

	public enum ParameterCategory {
		SlipStream_Support, SlipStream_Basics, SlipStream_Advanced
	}

	/**
	 * Encoded names. '_' here corresponds to '.' in config file.
	 */
	public enum RequiredParameters {

		SLIPSTREAM_BASE_URL(
				CljElasticsearchHelper.getParameterDescription("slipstream.base.url")),

		SLIPSTREAM_UPDATE_CLIENTURL(
				CljElasticsearchHelper.getParameterDescription("slipstream.update.clienturl")),

		SLIPSTREAM_PRS_ENDPOINT(
				CljElasticsearchHelper.getParameterDescription("slipstream.prs.endpoint")),

		SLIPSTREAM_PRS_ENABLE(
				CljElasticsearchHelper.getParameterDescription("slipstream.prs.enable")),

		SLIPSTREAM_UPDATE_CLIENTBOOTSTRAPURL(
				CljElasticsearchHelper.getParameterDescription("slipstream.update.clientbootstrapurl")),

		CLOUD_CONNECTOR_CLASS(
				CljElasticsearchHelper.getParameterDescription("cloud.connector.class")),

		CLOUD_CONNECTOR_LIBRARY_LIBCLOUD_URL(
				CljElasticsearchHelper.getParameterDescription("cloud.connector.library.libcloud.url")),

		CLOUD_CONNECTOR_ORCHESTRATOR_PUBLICSSHKEY(
				CljElasticsearchHelper.getParameterDescription("cloud.connector.orchestrator.publicsshkey")),

		CLOUD_CONNECTOR_ORCHESTRATOR_PRIVATESSHKEY(
				CljElasticsearchHelper.getParameterDescription("cloud.connector.orchestrator.privatesshkey")),

		SLIPSTREAM_VERSION(
				CljElasticsearchHelper.getParameterDescription("slipstream.version")),

		SLIPSTREAM_REPORTS_LOCATION(
				CljElasticsearchHelper.getParameterDescription("slipstream.reports.location")),

		SLIPSTREAM_REGISTRATION_EMAIL(
				CljElasticsearchHelper.getParameterDescription("slipstream.registration.email")) {
			@Override
			public void validate(String value) {
				super.validate(value);
				isValidEmail(value);
			}
		},

		SLIPSTREAM_MAIL_HOST(
				CljElasticsearchHelper.getParameterDescription("slipstream.mail.host")),

		SLIPSTREAM_MAIL_PORT(
				CljElasticsearchHelper.getParameterDescription("slipstream.mail.port")) {
			@Override
			public void validate(String value) {
				super.validate(value);
				isValidPort(value);
			}
		},

		SLIPSTREAM_MAIL_USERNAME(
				CljElasticsearchHelper.getParameterDescription("slipstream.mail.username")),

		SLIPSTREAM_MAIL_PASSWORD(
				CljElasticsearchHelper.getParameterDescription("slipstream.mail.password")),

		SLIPSTREAM_MAIL_SSL(
				CljElasticsearchHelper.getParameterDescription("slipstream.mail.ssl")),

		SLIPSTREAM_MAIL_DEBUG(
				CljElasticsearchHelper.getParameterDescription("slipstream.mail.debug")),

		SLIPSTREAM_SUPPORT_EMAIL(
				CljElasticsearchHelper.getParameterDescription("slipstream.support.email")) {
			@Override
			public void validate(String value) {
				super.validate(value);
				isValidEmail(value);
			}
		},

		SLIPSTREAM_REGISTRATION_ENABLE(
				CljElasticsearchHelper.getParameterDescription("slipstream.registration.enable")),

		SLIPSTREAM_SERVICE_CATALOG_ENABLE(
				CljElasticsearchHelper.getParameterDescription("slipstream.service.catalog.enable")),

		SLIPSTREAM_METERING_HOSTNAME(
				CljElasticsearchHelper.getParameterDescription("slipstream.metering.hostname")),

		SLIPSTREAM_METERING_ENABLE(
				CljElasticsearchHelper.getParameterDescription("slipstream.metering.enable")),

		SLIPSTREAM_QUOTA_ENABLE(
				CljElasticsearchHelper.getParameterDescription("slipstream.quota.enable"));

		private final String description;
		private final ParameterCategory category;
		private final String instructions;
		private final ParameterType type;
		private final boolean readonly;

		public String getInstructions() {
			return instructions;
		}

		private RequiredParameters(ServiceConfigurationParameter scp) {
			this.description = scp.getDescription();
			this.category = ParameterCategory.valueOf(scp.getCategory());
			this.instructions = scp.getInstructions();
			this.type = scp.getType();
			this.readonly = scp.isReadonly();
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

		/**
		 * Convert string to enum name where word separators are
		 * converted from . to _ and upper cased.
		 */
		public static String getEnum(String name) {
			return name.replace(".", "_").toUpperCase();
		}
	}

	//@Id
	String id;

	public ServiceConfiguration() {
		setId();
	}

	@Override
	@ElementMap(name = "parameters", required = false, valueType = ServiceConfigurationParameter.class)
	public Map<String, ServiceConfigurationParameter> getParameters() {
		return parameters;
	}

	@Override
	@ElementMap(name = "parameters", required = false, valueType = ServiceConfigurationParameter.class)
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

		if (null == parameter) {
			return;
		}

		validateParameter(parameter);

		Map<String, ServiceConfigurationParameter> parameters = getParameters();

		parameter.setContainer(this);
		parameters.put(parameter.getName(), parameter);
	}

	public Parameter<ServiceConfiguration> getParameter(String name,
			ParameterCategory category) {
		Parameter<ServiceConfiguration> parameter = getParameter(name);
		if (parameter != null && parameter.getCategory().equals(category.name())) {
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
			if (parameter.getCategory().equals(category.name())) {
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

	private void setId() {
		id = RESOURCE_URI;
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

	private static String CLJ_NS_SERVICE_CONFIG = CljElasticsearchHelper.NS_SERIALIZERS_SERVICE_CONFIG;

	public static ServiceConfiguration load() {
		IFn load = CljElasticsearchHelper.getLoadFn(CLJ_NS_SERVICE_CONFIG);
		return (ServiceConfiguration) load.invoke();
	}

	public ServiceConfiguration store() {
		validate();
		IFn store = CljElasticsearchHelper.getStoreFn(CLJ_NS_SERVICE_CONFIG);
		return (ServiceConfiguration) store.invoke(this);
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
