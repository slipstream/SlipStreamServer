package com.sixsq.slipstream.connector;

/**
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

/**
 * Classes that implement this interface can instantiate a cloud connector for a particular cloud service.  These stub
 * classes are intended to be discovered automatically via the ConnectorStubLoader class.
 *
 * Classes implementing this interface _must_ be immutable for thread safety.
 */
public interface DiscoveryConnectorService {

    /**
     * Returns the name of the cloud service that can be accessed via the Connectors created by this stub.  This method
     * must return a non-empty String.
     *
     * @return cloud service name (non-empty string)
     */
    public String getCloudServiceName();

    /**
     * Creates a new instance of a Connector for the given cloud service.  If the instanceName is null, then the
     * Connector instance should be configured with the default name.
     *
     * @param instanceName
     * @return Connector instance
     */
    public Connector getInstance(String instanceName);

    /**
     * Initializes the ConnectorStub at service initialization time.  The stub should perform any global initialization
     * needed by cloud connector instances.  This will be called only once per ConnectorStub.
     */
    public void initialize();

    /**
     * Shuts down and frees any allocated resources associated with the ConnectorStub.  This method will be called once
     * just before the shutdown of the SlipStream service.  Connectors generated from the ConnectorStub should not be
     * used after a call to this method.
     */
    public void shutdown();
}
