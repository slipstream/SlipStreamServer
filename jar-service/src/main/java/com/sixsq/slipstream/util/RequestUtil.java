package com.sixsq.slipstream.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.restlet.Request;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.Util;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.resource.BaseResource;

public class RequestUtil {

	public static final String TYPE_KEY = "type";

	public static final String REQUEST_KEY = "request";
	public static final String QUERY_PARAMETERS_KEY = "query-parameters";
	public static final String URL_KEY = "url";

	public static User getUserFromRequest(Request request)
			throws ConfigurationException, ValidationException {
		return (User) request.getAttributes().get(User.REQUEST_KEY);
	}

	public static String getRolesFromRequest(Request request)
			throws ConfigurationException, ValidationException {
		return (String) request.getAttributes().getOrDefault(User.REQUEST_ROLES_KEY,"USER");
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

	public static String constructAbsolutePath(Request request, String relativePath) {
		String absolutePath = ResourceUriUtil.getBaseUrl(request);
		if (relativePath != null) {
			absolutePath += relativePath;
		}
		return absolutePath;
	}

	/**
	 * Options specification:
	 * {
	 *     type:   "chooser",     // possible values: "edit", "new", "view", chooser (default if missing: "view")
	 *     lang:   "en",          // possible values: "en", "fr"                     (default if missing: "en")
	 *     theme:  "helixnebula", // possible values: "helixnebula", "default"       (default if missing: "default")
	 *     request: {
	 *         url: "/path/to/requested/resource",
	 *         query-parameters: {
	 *             query-param-1-name: "query-param-1-value",
	 *             query-param-2-name: "query-param-2-value",
	 *             ...
	 *         }
	 *     }
	 * }
	 */
	public static Map<String, Object> constructOptions(Request request) {
		Map<String, Object> options = new HashMap<String, Object>();

		String type = constructTransformationType(request);
		options.put(TYPE_KEY, type);

		//options.put("lang", "en"); // TODO
		//options.put("theme", "default"); // TODO

		Map<String, Object> requestMap = new HashMap<String, Object>();

		requestMap.put(URL_KEY, request.getOriginalRef().getPath());

		Map<String, String> queryMap = request.getResourceRef().getQueryAsForm().getValuesMap();
		requestMap.put(QUERY_PARAMETERS_KEY, queryMap);

		options.put(REQUEST_KEY, requestMap);

		return options;
	}

	private static String constructTransformationType(Request request) {

		String type = "view";

		type = setTypeIfTrue(request, BaseResource.CHOOSER_KEY, type);
		type = setTypeIfTrue(request, BaseResource.EDIT_KEY, type);
		type = setTypeIfTrue(request, BaseResource.NEW_KEY, type);

		return type;
	}

	private static String setTypeIfTrue(Request request, String queryKey, String defaultType) {
		String type = defaultType;
		Parameter parameter = request.getResourceRef().getQueryAsForm().getFirst(queryKey);
		if(parameter != null && "true".equals(parameter.getValue())) {
			type = queryKey;
		}
		// new might be in the path, instead of the query part of the url
		if(BaseResource.NEW_KEY.equals(queryKey) && request.getResourceRef().getPath().endsWith("/" + BaseResource.NEW_KEY)) {
			type = queryKey;
		}
		return type;
	}

	public static String getQueryValue(Request request, String name) {
		return getQueryValue(request, name, null);
	}

	public static String getQueryValue(Request request, String name, String defaultValue) {
		String value = null;
		Reference ref = request.getResourceRef();

		if (ref != null) {
			value = ref.getQueryAsForm().getFirstValue(name, defaultValue);
		}

		return value;
	}

	public static int getOffset(Request request) {
		String offsetAttr = getQueryValue(request, BaseResource.PAGING_OFFSET_KEY);

		int offset = 0;
		if (offsetAttr != null) {
			try {
				offset = Integer.parseInt(offsetAttr);
			} catch (NumberFormatException e) {
				Util.throwClientBadRequest("Invalid format for the offset attribute");
			}
			if (offset < 0) {
				Util.throwClientBadRequest("The value for the offset attribute should be positive");
			}
		}
		return offset;
	}

	public static int getLimit(Request request) {
		return getLimit(request, BaseResource.LIMIT_DEFAULT, BaseResource.LIMIT_MAX);
	}

	public static int getLimit(Request request, int defaultLimit, int max) {
		String limitAttr = getQueryValue(request, BaseResource.PAGING_LIMIT_KEY);

		int limit = defaultLimit;
		if (limitAttr != null) {
			try {
				limit = Integer.parseInt(limitAttr);
			} catch (NumberFormatException e) {
				Util.throwClientBadRequest("Invalid format for the limit attribute");
			}
			if (limit < 1 || limit > max) {
				Util.throwClientBadRequest("The value for the limit attribute should be between 1 and 500");
			}
		}
		return limit;
	}

	public static String getCIMIFilter(Request request) {
		return getQueryValue(request, BaseResource.CIMI_FILTER_KEY);
	}

	public static String getActiveOnly(Request request) {
		return getQueryValue(request, BaseResource.ACTIVE_ONLY_KEY);
	}

}
