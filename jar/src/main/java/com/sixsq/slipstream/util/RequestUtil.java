package com.sixsq.slipstream.util;

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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.restlet.Request;
import org.restlet.data.Reference;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;

public class RequestUtil {

	public static final String CONFIGURATION_KEY = "com.sixsq.slipstream.CONFIGURATION";

	public static final String SVC_CONFIGURATION_KEY = "com.sixsq.slipstream.SVC_CONFIGURATION";

	public static final String ENTITY_MANAGER_KEY = "ENTITY_MANAGER";

	private RequestUtil() {
	}

	public static Reference getBaseRefSlash(Request request) {
		String baseUrlSlash = getBaseUrlSlash(request);
		return new Reference(baseUrlSlash);
	}

	public static String getBaseUrlSlash(Request request) {
		Reference rootRef = request.getRootRef();
		String url = rootRef.toString();
		if (!url.endsWith("/")) {
			url += "/";
		}
		return url;
	}

	public static String getBaseUrl(Request request) {
		Reference rootRef = request.getRootRef();
		String url = rootRef.toString();
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}

	public static Reference getBaseRef(Request request) {
		String baseUrl = getBaseUrl(request);
		return new Reference(baseUrl);
	}

	public static String extractResourceUri(Request request) {
		Reference resourceRef = request.getResourceRef();
		Reference baseRefSlash = new Reference(getBaseUrlSlash(request));
		Reference resourceUrlRef = resourceRef.getRelativeRef(baseRefSlash);
		String uri = resourceUrlRef.getHierarchicalPart();
		return uri.equals(".") ? "module/" : uri;
	}

	public static Configuration getConfigurationFromRequest(Request request) {
		try {
			Map<String, Object> attributes = request.getAttributes();
			Object value = attributes.get(RequestUtil.CONFIGURATION_KEY);
			if (value == null) {
				throw new SlipStreamRuntimeException(
						"configuration not found in request");
			}
			return (Configuration) value;
		} catch (ClassCastException e) {
			throw new SlipStreamRuntimeException(e.getMessage());
		}
	}

	public static ServiceConfiguration getServiceConfigurationFromRequest(
			Request request) {
		try {
			Map<String, Object> attributes = request.getAttributes();
			Object value = attributes.get(RequestUtil.SVC_CONFIGURATION_KEY);
			if (value == null) {
				throw new SlipStreamRuntimeException(
						"service configuration not found in request");
			}
			return (ServiceConfiguration) value;
		} catch (ClassCastException e) {
			throw new SlipStreamRuntimeException(e.getMessage());
		}
	}

	public static void addConfigurationToRequest(Request request) throws ConfigurationException {
		Map<String, Object> attributes = request.getAttributes();

		Configuration configuration = Configuration.getInstance();
//		configuration.update();
		attributes.put(CONFIGURATION_KEY, configuration);
		request.setAttributes(attributes);
		request.getAttributes().put(RequestUtil.SVC_CONFIGURATION_KEY, configuration.getParameters());

	}

	public static void addServiceConfigurationToRequest(
			AtomicReference<ServiceConfiguration> cfgReference, Request request) {
		Map<String, Object> attributes = request.getAttributes();

		if (cfgReference == null) {
			ServiceConfiguration cfg = ServiceConfiguration.load();
			cfgReference = new AtomicReference<ServiceConfiguration>(cfg);
		}

		attributes.put(SVC_CONFIGURATION_KEY, cfgReference.get());
		request.setAttributes(attributes);

	}

}
