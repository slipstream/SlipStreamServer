package com.sixsq.slipstream.configuration;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.persistence.NoResultException;

import org.restlet.data.Protocol;
import org.restlet.data.Reference;

import slipstream.ui.views.Representation;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.factory.ParametersFactory;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ParameterType;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfiguration.RequiredParameters;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;

/**
 * This singleton class is the interface to the service configuration. It
 * handles reading some services configuration files and makes the parameters
 * available via the getProperty() method. The configuration can be reloaded
 * from the UI, which purges the persisted configuration in the db. When the
 * service is started for the first time, some configuration files are loaded and
 * persisted.
 *
 * The system first reads a set of default values. These may be overridden by a
 * user specified configuration files.
 *
 * After setting the defaults, this class check if the "slipstream.config.dir"
 * system property is set. If it is, it will be used as a name to check the
 * configuration directory exists. Users should probably use an absolute path
 * when setting the system property. A relative name will be resolved via the
 * JVM and may not have the desired effect.
 *
 * If the system property is not set, then the configuration directory is searched
 * in the user's home area.
 *
 * If no configuration directory is found, then just the default configuration is
 * used. The default configuration is likely to be incomplete (some critical
 * properties will not have reasonable default values) and will likely to cause
 * the service to fail during initialization elsewhere.
 *
 * The singleton instance of this class is immutable, so clients are encouraged
 * to cache a copy of instance returned by getInstance() rather than reinvoking
 * the method.
 *
 */
public class Configuration {

	/**
	 * Singleton instance. Since this will not be used heavily after startup use
	 * lazy, synchronized instantiation.
	 */
	private static Configuration instance = null;

	/**
	 * Name of the system property used to set the location/name of the
	 * configuration file. It is suggested that users specify an absolute path
	 * name when using this system property.
	 */
	private static final String CONFIG_SYSTEM_PROPERTY = "slipstream.config.dir";

	/**
	 * Home config directory.
	 */
	private static final String HOME_CONFIG_DIRECTORY = ".slipstream";

	private ServiceConfiguration serviceConfiguration = new ServiceConfiguration();

	private Reference baseRef;

	private int defaultPort = 80;
	private int defaultSecurePort = 443;

	/**
	 * SlipStream version number derived from the slipstream.version property in
	 * the property default file.
	 */
	public String version;

	/**
	 * The base URL of the SlipStream service (without a trailing slash) as a
	 * String. This value will never be null and will never have a trailing
	 * slash.
	 */
	public String baseUrl;

	public static boolean isEnabled(String key) throws ValidationException {
		return Boolean.parseBoolean(Configuration.getInstance().getProperty(key));
	}

	public static boolean isQuotaEnabled() throws ValidationException {
		return isEnabled(ServiceConfiguration.RequiredParameters.SLIPSTREAM_QUOTA_ENABLE.getName());
	}

	public static boolean isMetricsLoggerEnabled() throws ValidationException {
		return isEnabled(ServiceConfiguration.RequiredParameters.SLIPSTREAM_METRICS_LOGGER_ENABLE.getName());
	}

	public static boolean isMetricsGraphiteEnabled() throws ValidationException {
		return isEnabled(ServiceConfiguration.RequiredParameters.SLIPSTREAM_METRICS_GRAPHITE_ENABLE.getName());
	}

	public static boolean getMeteringEnabled() throws ConfigurationException, ValidationException {
		Configuration config = Configuration.getInstance();
		Boolean enabled = Boolean.parseBoolean(config.getProperty(
				ServiceConfiguration.RequiredParameters.SLIPSTREAM_METERING_ENABLE.getName(), "true"));
		return enabled;
	}

	/**
	 * Return the singleton instance of a Configuration object. This method must
	 * be synchronized to ensure that only one instance of this class is
	 * constructed.
	 *
	 * @return singleton Configuration instance
	 *
	 * @throws ConfigurationException
	 *             if there is an error when reading the configuration
	 * @throws ValidationException
	 */
	public static synchronized Configuration getInstance() throws ConfigurationException, ValidationException {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}

	/**
	 * Private constructor, called only from getInstance(), ensures that this is
	 * a singleton class. The constructor will first load the default properties
	 * and then search for the user-specified configuration file.
	 *
	 * @throws ConfigurationException
	 *             if an error occurs during the default initialization or when
	 *             searching for user-specified configuration file
	 * @throws ValidationException
	 */
	private Configuration() throws ConfigurationException, ValidationException {

		try {
			ServiceConfiguration serviceConfiguration = ServiceConfiguration.load();
			update(serviceConfiguration.getParameters());
		} catch (NoResultException ex) {
			reset();
			store();
		}

	}

	private void postProcessParameters() throws ConfigurationException, ValidationException {
		// Extract the SlipStream version number from the tag. Add this as a
		// property in the configuration. Do this at the end so that a user
		// cannot override the value.
		extractAndSetVersion();

		// Validate the base URL (and associated Reference) and cache the
		// results.
		baseRef = initializeBaseRef(RequiredParameters.SLIPSTREAM_BASE_URL.getName());

		// Calculate the base path. This value must both begin and end with a
		// slash and cannot be null or empty.
		String pathSlash = baseRef.getPath();
		if (pathSlash == null) {
			pathSlash = "/";
		}
		if (!pathSlash.startsWith("/")) {
			pathSlash = "/" + pathSlash;
		}
		if (!pathSlash.endsWith("/")) {
			pathSlash += "/";
		}

		// Setup commonly used strings related to the base URL.
		baseUrl = initializeBaseUrl(baseRef);

		setMandatoryToAllParameters();
	}

	private void extractAndSetVersion() throws ValidationException {
		String versionRequiredParameter = RequiredParameters.SLIPSTREAM_VERSION.getName();

		ServiceConfigurationParameter versionParameter = getParameters().getParameter(
				versionRequiredParameter);
		if (versionParameter == null) {
			throw (new ConfigurationException("Missing mandatory configuration parameter "
					+ versionRequiredParameter));
		}
		version = versionParameter.getValue();
		versionParameter.setReadonly(true);
		Representation.setReleaseVersion(version);
	}

	private void setMandatoryToAllParameters() {
		for (ServiceConfigurationParameter parameter : serviceConfiguration.getParameterList()) {
			parameter.setMandatory(true);
		}
	}

	/**
	 * Merge a list of parameters with the parameters provided by the
	 * connectors. loaded from file with the parameters known by the
	 * connector(s) specified in the config file. Build the properties using the
	 * first element of the property name '.' separated. This allows the config
	 * file to provide properties that are not known to a connector defined in
	 * the ServiceConfiguration.AllowedParameter.CLOUD_CONNECTOR_CLASS property
	 * of the config file. Since connector class names can be added at anytime
	 * during the lifetime of a SlipStream server instance.
	 *
	 */
	private void mergeWithParametersFromConnectors() {

		// We might have loaded a new list of connector classes, so
		// reset the connectors
		ConnectorFactory.resetConnectors();

		String[] connectorClassNames = getConnectorClassNames();

		Map<String, ServiceConfigurationParameter> connectorsParameters;
		try {
			connectorsParameters = ParametersFactory.getServiceConfigurationParametersTemplate(connectorClassNames);
		} catch (ValidationException e) {
			throw new ConfigurationException(e.getMessage());
		}

		// Loop around the connector parameters and add them to the list if
		// they are not there, otherwise reset their fields, with the exception
		// of the value
		for (ServiceConfigurationParameter p : connectorsParameters.values()) {

			ServiceConfigurationParameter parameter = p;
			if (serviceConfiguration.parametersContainKey(p.getName())) {
				parameter = serviceConfiguration.getParameters().get(p.getName());
				parameter.setDescription(p.getDescription());
				parameter.setMandatory(p.isMandatory());
				parameter.setCategory(p.getCategory());
				parameter.setType(p.getType());
				if (parameter.getType() == ParameterType.Enum) {
					parameter.setEnumValues(p.getEnumValues());
				}
			}

			serviceConfiguration.setParameter(parameter);
		}
	}

	protected ServiceConfigurationParameter createParameter(String value, String parameterFormattedKeyName,
			String description, String category) throws ValidationException {
		ServiceConfigurationParameter parameter = new ServiceConfigurationParameter(parameterFormattedKeyName, value);

		parameter.setDescription(description);
		parameter.setMandatory(true);

		parameter.setCategory(category);
		return parameter;
	}

	private Map<String, ServiceConfigurationParameter> convertPropertiesToParameters(Properties properties)
			throws ValidationException {
		Map<String, ServiceConfigurationParameter> parameters = new HashMap<String, ServiceConfigurationParameter>();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();

			ServiceConfigurationParameter parameter = createParameter(value, key, null, extractCategory(key));

			parameters.put(key, parameter);
		}
		return parameters;
	}

	private String extractCategory(String key) {

		return key.split("\\.")[0];
	}

	protected String[] getConnectorClassNames() {
		String cloudConnectorClassNameParameterKey = ServiceConfiguration.RequiredParameters.CLOUD_CONNECTOR_CLASS
				.getName();

		if (!serviceConfiguration.parametersContainKey(cloudConnectorClassNameParameterKey)) {
			throw (new ConfigurationException(
					"Missing from the configuration file mandatory system configuration parameter: "
							+ cloudConnectorClassNameParameterKey));
		}
		String cloudConnectorClassNameParameterValue = serviceConfiguration.getParameters()
				.get(cloudConnectorClassNameParameterKey).getValue();

		return ConnectorFactory.splitConnectorClassNames(cloudConnectorClassNameParameterValue);
	}

	/**
	 * Utility method to search through possible locations of the configuration
	 * file and return the most appropriate one.
	 *
	 * @return File object containing configuration or null if none found
	 *
	 * @throws ConfigurationException
	 *             if any error occurs while reading configuration files
	 */
	public static File findConfigurationDirectory() throws ConfigurationException {

		// Check first if a system property is set that defines the location of
		// the configuration information.
		String name = System.getProperty(CONFIG_SYSTEM_PROPERTY);
		if (name != null) {
			return new File(name);
		}

		// Try the home area of the user.
		String home = System.getProperty("user.home");
		if (home != null) {
			File ssHomeDir = new File(home + File.separator + HOME_CONFIG_DIRECTORY);
			if (ssHomeDir.canRead()) {
				return ssHomeDir;
			}
		}

		// Nothing found
		return null;
	}

    public static Properties loadPropertiesFile(URI uri, Properties properties) {
        InputStream inputStream = null;
        try {
            URL url = uri.toURL();
            inputStream = url.openStream();
            properties.load(inputStream);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid configuration URL.");
        } catch (IOException e) {
            throw new ConfigurationException("Error loading configuration file.");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException consumed) {
                // Ignore errors on close.
            }
        }

        return properties;
    }

    public ServiceConfiguration getParameters() {
		return serviceConfiguration;
	}

	/**
	 * Retrieve the configuration value associated with the given key. Will
	 * return null if the key does not exist.
	 *
	 * @param key
	 *            parameter name
	 *
	 * @return value associated with key or null if the key does not exist
	 */
	public String getProperty(String key) {
		ServiceConfigurationParameter parameter = serviceConfiguration.getParameter(key);
		return (parameter == null ? null : parameter.getValue());
	}

	/**
	 * Retrieve the configuration value associated with the given key. Will
	 * return defaultValue if the key does not exist or is set to null.
	 *
	 * @param key
	 *            parameter name
	 * @param defaultValue
	 *
	 * @return value associated with key or null if the key does not exist
	 */
	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		return (value == null ? defaultValue : value);
	}

	/**
	 * Retrieve the configuration value associated with the given key or throw
	 * an exception if it does not exist.
	 *
	 * @param key
	 *
	 * @return value associated with the key
	 *
	 * @throws ConfigurationException
	 *             if there is no value associated with the given key
	 */
	public String getRequiredProperty(String key) throws ConfigurationException {
		String value = getProperty(key);
		if (value == null) {
			throw new ConfigurationException("missing configuration property: " + key);
		}
		return value;
	}

	/**
	 * Returns a Reference containing a copy of the base Reference. Because
	 * Reference objects are mutable, copies of the internal object must be
	 * returned to guarantee consistency of the Configuration object.
	 *
	 * @return copy of the base Reference for the service
	 */
	public Reference getBaseRef() {
		return new Reference(baseRef);
	}

	/**
	 * Retrieve the property containing the base URL, validate it, and return a
	 * Reference containing the value.
	 *
	 * The path in the returned reference will always have a trailing slash.
	 *
	 * @param propertyName
	 *            name of the property holding the base URL
	 *
	 * @return Reference containing the validated base URL
	 *
	 * @throws ConfigurationException
	 *             if the property does not exist or the contained value is
	 *             invalid
	 */
	private Reference initializeBaseRef(String propertyName) throws ConfigurationException {

		String uri = getRequiredProperty(propertyName);

		// Create a Reference so that it can do all the work of parsing the
		// input URI.
		Reference reference = null;
		try {
			reference = new Reference(uri);
		} catch (IllegalArgumentException e) {
			throw new ConfigurationException("invalid uri: " + uri);
		}

		// Check that the scheme is either http or https. Set the default port
		// number accordingly.
		String scheme = reference.getScheme(true);
		int port = -1;
		if ("http".equals(scheme)) {
			port = 80;
		} else if ("https".equals(scheme)) {
			port = 443;
		} else {
			scheme = (scheme == null) ? "" : scheme;
			throw new ConfigurationException("invalid scheme: '" + scheme + "'");
		}

		// Pull out the port number, if specified in the URL.
		int uriPort = reference.getHostPort();

		// Use port values in the following order: explicit port in URI, default
		// port specified as argument, or default scheme port.
		if (uriPort > 0) {
			port = uriPort;
		} else {
			if (reference.getSchemeProtocol() == Protocol.HTTP) {
				port = defaultPort;
			} else if (reference.getSchemeProtocol() == Protocol.HTTPS) {
				port = defaultSecurePort;
			}
		}

		// Pull out the host name.
		String host = reference.getHostDomain();
		if (host == null) {
			throw new ConfigurationException("invalid host in root URL");
		}

		// Pull out the path for the URL. Ensure that this is always an absolute
		// path and that the path has a trailing slash.
		String rootPath = reference.getPath();
		if (rootPath == null) {
			rootPath = "/";
		}
		if (!rootPath.endsWith("/")) {
			rootPath = rootPath + "/";
		}
		if (!rootPath.startsWith("/")) {
			rootPath = "/" + rootPath;
		}

		// Reconstitute the root URL, thereby ignoring any extraneous parts of
		// the specified configuration parameter.
		return new Reference(scheme, host, port, rootPath, null, null);
	}

	/**
	 * Create a string representation of the base URL. This value will have all
	 * trailing slashes removed.
	 *
	 * @param baseRef
	 * @return
	 */
	private String initializeBaseUrl(Reference baseRef) {

		String url = baseRef.toString();

		// TODO: This assumption should be fixed in the rest of the code.
		// Strip any trailing slashes.
		while (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		return url;
	}

	/**
	 * Constructs a URL to a service from configuration information.
	 *
	 * @param configServiceName
	 *            name of service section in configuration file
	 * @return complete URL
	 *
	 * @throws ConfigurationException
	 */
	public String getServiceUrl(String configServiceName) throws ConfigurationException {

		String url;
		try {
			url = getRequiredProperty(configServiceName + ".url") + getRequiredProperty(configServiceName + ".service");
		} catch (ConfigurationException e) {
			url = getDefaultServiceUrl(configServiceName);
		}
		return url;
	}

	/**
	 * Constructs a url to the default service root based on configuration
	 * information.
	 *
	 * @param configServiceName
	 *            name of service section in configuration file
	 * @return complete url
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ConfigurationException
	 */
	public String getDefaultServiceUrl(String configServiceName) throws ConfigurationException {

		return baseUrl + getRequiredProperty(configServiceName + ".service");
	}

	/**
	 * Forces to re-read the configuration from file, removing all persisted
	 * state. Re-load the default parameters from the configured connectors and
	 * validate that the required parameters are present.
	 *
	 * @throws ConfigurationException
	 * @throws ValidationException
	 */
	public void reset() throws ConfigurationException, ValidationException {
		serviceConfiguration = new ServiceConfiguration();
		mergeWithParametersFromConnectors();
		postProcessParameters();
		validateRequiredParameters();
		resetRequiredParameterDefinition();
	}

	/**
	 * First load config file (just in case there are new required parameters)
	 * The overwrite them with content from the db (if previously persisted)
	 * Then process and validate
	 *
	 * @throws ValidationException
	 * @throws ConfigurationException
	 */
	public void update(Map<String, ServiceConfigurationParameter> parameters) throws ConfigurationException,
			ValidationException {
		serviceConfiguration.setParameters(parameters);
		mergeWithParametersFromConnectors();
		postProcessParameters();
		validateRequiredParameters();
		resetRequiredParameterDefinition();
	}

	private void validateRequiredParameters() {
		getParameters().validate();
	}

	private void resetRequiredParameterDefinition() {
		for (RequiredParameters required : ServiceConfiguration.RequiredParameters.values()) {
			ServiceConfigurationParameter target = serviceConfiguration.getParameter(required.getName());
			target.setCategory(required.getCategory().name());
			target.setType(required.getType());
			target.setDescription(required.getDescription());
			target.setInstructions(required.getInstruction());
			target.setMandatory(true);
		}
	}

	public void store() {
		serviceConfiguration = (ServiceConfiguration) serviceConfiguration.store();
	}
}
