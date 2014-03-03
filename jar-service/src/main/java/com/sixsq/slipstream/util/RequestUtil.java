package com.sixsq.slipstream.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.restlet.Request;
import org.restlet.data.Reference;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.User;

public class RequestUtil {

	public static User getUserFromRequest(Request request)
			throws ConfigurationException, ValidationException {
		User user = null;
		try {
			user = User.loadByName(request.getClientInfo().getUser().getName(),
					RequestUtil.getConfigurationFromRequest(request).getParameters());
		} catch (NullPointerException ex) {
			// user not authenticated
		}
		return user;
	}

	public static void addServiceConfigurationToRequest(
			AtomicReference<ServiceConfiguration> cfgReference, Request request) {
		Map<String, Object> attributes = request.getAttributes();
	
		if (cfgReference == null) {
			ServiceConfiguration cfg = ServiceConfiguration.load();
			cfgReference = new AtomicReference<ServiceConfiguration>(cfg);
		}
	
		attributes.put(ResourceUriUtil.SVC_CONFIGURATION_KEY, cfgReference.get());
		request.setAttributes(attributes);
	
	}

	public static void addConfigurationToRequest(Request request)
			throws ConfigurationException, ValidationException {
		Map<String, Object> attributes = request.getAttributes();
	
		Configuration configuration = Configuration.getInstance();
		attributes.put(ResourceUriUtil.CONFIGURATION_KEY, configuration);
		request.setAttributes(attributes);
		request.getAttributes().put(ResourceUriUtil.SVC_CONFIGURATION_KEY,
				configuration.getParameters());
	
	}

	public static ServiceConfiguration getServiceConfigurationFromRequest(
			Request request) {
		try {
			Map<String, Object> attributes = request.getAttributes();
			Object value = attributes.get(ResourceUriUtil.SVC_CONFIGURATION_KEY);
			if (value == null) {
				throw new SlipStreamRuntimeException(
						"service configuration not found in request");
			}
			return (ServiceConfiguration) value;
		} catch (ClassCastException e) {
			throw new SlipStreamRuntimeException(e.getMessage());
		}
	}

	public static Configuration getConfigurationFromRequest(Request request) {
		try {
			Map<String, Object> attributes = request.getAttributes();
			Object value = attributes.get(ResourceUriUtil.CONFIGURATION_KEY);
			if (value == null) {
				throw new SlipStreamRuntimeException(
						"configuration not found in request");
			}
			return (Configuration) value;
		} catch (ClassCastException e) {
			throw new SlipStreamRuntimeException(e.getMessage());
		}
	}
	
	public static String constructAbsolutePath(String relativePath) {
		return relativePath;
		/*try {
			Configuration configuration = Configuration.getInstance();
			Reference baseRef = configuration.getBaseRef();
			return new Reference(baseRef, relativePath).getTargetRef().toString();
		} catch (ConfigurationException e) { 
			return relativePath;
		} catch (ValidationException e) { 
			return relativePath;
		}*/
	}

}
