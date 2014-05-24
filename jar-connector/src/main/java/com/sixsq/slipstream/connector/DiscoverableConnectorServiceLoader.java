package com.sixsq.slipstream.connector;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2014 SixSq Sarl (sixsq.com)
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

import com.sixsq.slipstream.util.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * This class will dynamically load all DiscoverableConnectorService implementations available from the classpath via
 * the standard ServiceLoader facilities of the JVM (1.6+).
 *
 * To be visible, the containing jar file must be correctly configured.  Each jar file with DiscoverableConnectorService
 * implementations must contain the file:
 *
 * META-INF/services/com.sixsq.slipstream.connector.DiscoverableConnectorService
 *
 * This file contains a list (newline separated) of concrete class names that implement this interface.  This file must
 * be UTF-8 encoded.
 */
public final class DiscoverableConnectorServiceLoader {

    private static final ServiceLoader<DiscoverableConnectorService> loader = ServiceLoader
            .load(DiscoverableConnectorService.class);

    private static final Map<String, DiscoverableConnectorService> svcs;

    private static final List<String> cloudServiceNames;

    private static final String loadMessageFmt = "loaded cloud connector %s for cloud %s";

    private static final String removedMessageFmt = "removed cloud connector %s for cloud %s";

    static {

        // Create map of DiscoverableConnectorService objects keyed by the cloud service name.
        Map<String, DiscoverableConnectorService> impls = new HashMap<String, DiscoverableConnectorService>();
        for (DiscoverableConnectorService svc : loader) {
            String key = svc.getCloudServiceName().toLowerCase();
            DiscoverableConnectorService existing = impls.put(key, svc);
            if (existing != null) {
                Logger.warning(String.format(removedMessageFmt, existing.getClass().getName(), key));
            }
            Logger.info(String.format(loadMessageFmt, svc.getClass().getName(), key));
        }
        svcs = Collections.unmodifiableMap(impls);

        // Create an immutable, sorted list of all cloud service names.
        List<String> names = new ArrayList<String>();
        names.addAll(svcs.keySet());
        Collections.sort(names);
        cloudServiceNames = Collections.unmodifiableList(names);

    }

    public static void initializeAll() {
        for (DiscoverableConnectorService svc : svcs.values()) {
            svc.initialize();
        }
    }

    public static void shutdownAll() {
        for (DiscoverableConnectorService svc : svcs.values()) {
            svc.shutdown();
        }
    }

    public static DiscoverableConnectorService getConnectorService(String cloudServiceName) {
        String key = ConnectorFactory.convertClassNameToServiceName(cloudServiceName).toLowerCase();
        return svcs.get(key);
    }

    public static List<String> getCloudServiceNames() {
        return cloudServiceNames;
    }

}
