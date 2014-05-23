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
 * This class will dynamically load all ConnectorStub implementations available from the classpath via the standard
 * ServiceLoader facilities of the JVM (1.6+).
 *
 * To be visible, the containing jar file must be correctly configured.  Each jar file with ConnectorStub
 * implementations must contain the file:
 *
 * META-INF/services/com.sixsq.slipstream.connector.ConnectorStub
 *
 * This file contains a list (newline separated) of concrete class names that implement this interface.  This file must
 * be UTF-8 encoded.
 */
public final class DiscoverableConnectorServiceLoader {

    private static final ServiceLoader<DiscoverableConnectorService> loader = ServiceLoader.load(DiscoverableConnectorService.class);

    private static final Map<String, DiscoverableConnectorService> stubs;

    private static final List<String> cloudServiceNames;

    private static final String loadMessageFmt = "loaded cloud connector %s for cloud %s";

    private static final String removedMessageFmt = "removed cloud connector %s for cloud %s";

    static {

        // Create map of ConnectorStub objects keyed by the cloud service name.
        Map<String, DiscoverableConnectorService> impls = new HashMap<String, DiscoverableConnectorService>();
        for (DiscoverableConnectorService stub : loader) {
            String key = stub.getCloudServiceName().toLowerCase();
            DiscoverableConnectorService existing = impls.put(key, stub);
            if (existing != null) {
                Logger.warning(String.format(removedMessageFmt, existing.getClass().getName(), key));
            }
            Logger.info(String.format(loadMessageFmt, stub.getClass().getName(), key));
        }
        stubs = Collections.unmodifiableMap(impls);

        // Create an immutable, sorted list of all cloud service names.
        List<String> names = new ArrayList<String>();
        names.addAll(stubs.keySet());
        Collections.sort(names);
        cloudServiceNames = Collections.unmodifiableList(names);

    }

    public static void initializeAllStubs() {
        for (DiscoverableConnectorService stub : stubs.values()) {
            stub.initialize();
        }
    }

    public static void shutdownAllStubs() {
        for (DiscoverableConnectorService stub : stubs.values()) {
            stub.shutdown();
        }
    }

    public static DiscoverableConnectorService getConnectorStub(String cloudServiceName) {
        String key = convertClassNameToServiceName(cloudServiceName).toLowerCase();
        return stubs.get(key);
    }

    public static List<String> getCloudServiceNames() {
        return cloudServiceNames;
    }

    // If the argument looks to be a class name, then derive the cloud service name from the class name.  The cloud
    // service name will be the penultimate value when split on periods.  If there are no periods in the value,
    // then just return the value itself.
    public static String convertClassNameToServiceName(String className) {
        String[] elements = className.split("\\.");
        if (elements.length > 1) {
            return elements[elements.length - 2];
        } else {
            return className;
        }
    }

}
