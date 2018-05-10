package com.sixsq.slipstream.es;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2016 SixSq Sarl (sixsq.com)
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

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.persistence.ServiceConfigurationParameter;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class CljElasticsearchHelper {

    private static Logger logger = Logger.getLogger(CljElasticsearchHelper.class.getName());

    public static final String NS_SERIALIZERS_UTILS = "com.sixsq.slipstream.db.serializers.utils";
    public static final String NS_SERIALIZERS_SERVICE_CONFIG = "com.sixsq.slipstream.db.serializers.service-config";
    public static final String NS_SERIALIZERS_SERVICE_CONFIG_IMPL = "com.sixsq.slipstream.db.serializers.service-config-impl";

    public static void setDbCrudImpl() {
        requireNs(NS_SERIALIZERS_UTILS);
        Clojure.var(NS_SERIALIZERS_UTILS, "init-db-binding").invoke();
    }

    /**
     * Connection to a external Elasticsearch defined by ES_HOST and ES_PORT env vars.
     */
    public static void init() {
        logger.info("Creating DB client and setting ES DB CRUD implementation.");
        // initialized in test server
        //setDbCrudImpl();
        initializeResourceTemplates();
    }

    public static void initTestDb() {
        addDefaultServiceConfigToDb();
        initializeResourceTemplates();
        pushServerConfig();
    }

    public static void initializeResourceTemplates() {
        requireNs(NS_SERIALIZERS_UTILS);
        logger.info("Initializing resource templates.");
        Clojure.var(NS_SERIALIZERS_UTILS, "initialize").invoke();
    }

    private static void addDefaultServiceConfigToDb() {
        logger.info("Adding default service configuration to ES DB.");
        // initialized in test server
        //setDbCrudImpl();
        requireNs(NS_SERIALIZERS_SERVICE_CONFIG_IMPL);
		Clojure.var(NS_SERIALIZERS_SERVICE_CONFIG_IMPL, "db-add-default-config").invoke();
    }

    private static File findConfigurationDirectory() throws ConfigurationException {
        String name = System.getProperty("slipstream.config.dir");
        if (name != null) {
            return new File(name);
        }
        return null;
    }
    /**
     * WARNING! Should only be used with tests.
     */
    private static void pushServerConfig() {
        File confDir = findConfigurationDirectory();
        if (null != confDir && confDir.exists()) {
            File conf = new File(confDir + File.separator + "slipstream.edn");
            if (conf.exists()) {
                logger.warning("You should NOT see this on production! Loading configuration file: " +
                        conf.getAbsolutePath());
                getFn(NS_SERIALIZERS_SERVICE_CONFIG_IMPL, "db-edit-config-from-file").invoke(conf.getAbsolutePath());
            }
        }

    }

    public static IFn getLoadFn(String ns) {
        return getFn(ns, "load");
    }

    public static IFn getStoreFn(String ns) {
        return getFn(ns, "store");
    }

    private static IFn getFn(String ns, String funcName) {
        requireNs(ns);
        return Clojure.var(ns, funcName);
    }

    private static void requireNs(String ns) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read(ns));
    }

    public static ServiceConfigurationParameter getParameterDescription(String paramName) {
        return (ServiceConfigurationParameter)
                getFn(NS_SERIALIZERS_SERVICE_CONFIG_IMPL, "get-sc-param-meta-only").invoke(
                        new ServiceConfiguration(), paramName);
    }

    public static ServiceConfigurationParameter getConnectorParameterDescription(String paramName) {
        return (ServiceConfigurationParameter)
                getFn(NS_SERIALIZERS_SERVICE_CONFIG_IMPL, "get-connector-param-from-template").invoke(
                        new ServiceConfiguration(), paramName);
    }

    public static List<ServiceConfigurationParameter> getConnectorParameters(String connectorName) {
        List<ServiceConfigurationParameter> scps = (List<ServiceConfigurationParameter>)
                getFn(NS_SERIALIZERS_SERVICE_CONFIG_IMPL, "get-connector-params-from-template").invoke(
                        new ServiceConfiguration(), connectorName);
        if (scps.size() == 0) {
            logger.warning("Loaded 0 connector parameters for connector '" + connectorName + "'");
        }
        return scps;
    }

}
