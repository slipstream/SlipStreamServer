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

public class CljElasticsearchHelper {

    public static final String NS_SERIALIZERS_UTILS = "com.sixsq.slipstream.db.serializers.utils";
    public static final String NS_SERIALIZERS_SERVICE_CONFIG = "com.sixsq.slipstream.db.serializers.service-config";
    public static final String NS_SERIALIZERS_SERVICE_CONFIG_IMPL = "com.sixsq.slipstream.db.serializers.service-config-impl";

    private static void createElasticsearchClient() {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read(NS_SERIALIZERS_UTILS));
        Clojure.var(NS_SERIALIZERS_UTILS, "set-es-client").invoke();
    }

    private static void requireNs(String ns) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read(ns));
    }

    private static void createTestDb() {
        requireNs(NS_SERIALIZERS_UTILS);
		Clojure.var(NS_SERIALIZERS_UTILS, "create-test-es-db").invoke();
    }

    private static void addDefaultServiceConfigToDb() {
        createElasticsearchClient();
        requireNs(NS_SERIALIZERS_SERVICE_CONFIG_IMPL);
		Clojure.var(NS_SERIALIZERS_SERVICE_CONFIG_IMPL, "db-add-default-config").invoke();
    }

    public static void createAndInitDb() {
        createTestDb();
        addDefaultServiceConfigToDb();
    }

    public static IFn getFn(String ns, String funcName) {
        requireNs(ns);
        return Clojure.var(ns, funcName);
    }

    public static IFn getLoadFn(String ns) {
        createElasticsearchClient();
        return getFn(ns, "load");
    }

    public static IFn getStoreFn(String ns) {
        createElasticsearchClient();
        return getFn(ns, "store");
    }
}
