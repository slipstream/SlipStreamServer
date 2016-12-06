package com.sixsq.slipstream.connector.local;

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

import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.ConnectorTestBase;
import com.sixsq.slipstream.connector.SystemConfigurationParametersFactoryBase;
import com.sixsq.slipstream.util.CommonTestUtil;
import org.junit.Test;

import java.util.List;

import static com.sixsq.slipstream.connector.DiscoverableConnectorServiceLoader.getCloudServiceNames;
import static com.sixsq.slipstream.connector.DiscoverableConnectorServiceLoader.getConnectorService;
import static com.sixsq.slipstream.connector.local.LocalConnector.CLOUD_SERVICE_NAME;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LocalConnectorTest extends ConnectorTestBase {

    @Test
    public void ensureConnectorIsLoaded() throws Exception {

        List<String> cloudServiceNames = getCloudServiceNames();
        assertThat(cloudServiceNames.size(), greaterThan(0));
        assertTrue(cloudServiceNames.contains(CLOUD_SERVICE_NAME));

        assertTrue(getConnectorService(CLOUD_SERVICE_NAME) instanceof LocalDiscoverableConnectorService);
    }

    @Test
    public void ensureConnectorFactoryFindsConnectorWithName() throws Exception {

        String configConnectorName = CLOUD_SERVICE_NAME;
        String cloudServiceName = CLOUD_SERVICE_NAME;
        SystemConfigurationParametersFactoryBase factory = new LocalSystemConfigurationParametersFactory();

        CommonTestUtil.lockAndLoadConnector(configConnectorName, cloudServiceName, factory);

        Connector c = ConnectorFactory.getConnector(cloudServiceName);
        assertTrue(c instanceof LocalConnector);
        assertEquals(LocalConnector.CLOUD_SERVICE_NAME, c.getCloudServiceName());

        ConnectorFactory.resetConnectors();
    }

    @Test
    public void ensureConnectorFactoryFindsConnectorWithClassName() throws Exception {

        String configConnectorName = LocalConnector.class.getCanonicalName();
        String cloudServiceName = CLOUD_SERVICE_NAME;
        SystemConfigurationParametersFactoryBase factory = new LocalSystemConfigurationParametersFactory();

        CommonTestUtil.lockAndLoadConnector(configConnectorName, cloudServiceName, factory);

        assertTrue(ConnectorFactory.getConnector(cloudServiceName) instanceof LocalConnector);

        ConnectorFactory.resetConnectors();
    }

    @Test
    public void checkConstructors() {
        assertThat(new LocalConnector().getConnectorInstanceName(), notNullValue());
        assertThat(new LocalConnector(null).getConnectorInstanceName(), notNullValue());
        assertThat(new LocalConnector("MyID").getConnectorInstanceName(), notNullValue());
    }
}
