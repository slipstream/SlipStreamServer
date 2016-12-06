package com.sixsq.slipstream.connector;

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

import com.sixsq.slipstream.util.CommonTestUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ConnectorFactoryTest extends ConnectorTestBase {

    @Test
    public void splitConnectorClassNames() {
        assertThat(ConnectorFactory.splitConnectorClassNames(null), is(new String[0]));
        assertThat(ConnectorFactory.splitConnectorClassNames(""), is(new String[0]));
        assertThat(ConnectorFactory.splitConnectorClassNames("\t\f "), is(new String[0]));
        assertThat(ConnectorFactory.splitConnectorClassNames("a,b,c"), is(new String[]{"a", "b", "c"}));
        assertThat(ConnectorFactory.splitConnectorClassNames("a,b,c "), is(new String[]{"a", "b", "c "}));
        assertThat(ConnectorFactory.splitConnectorClassNames("a,b, ,c,"), is(new String[]{"a", "b", " ", "c"}));
        assertThat(ConnectorFactory.splitConnectorClassNames("a:one,b:two,c"), is(new String[]{"a:one", "b:two", "c"}));
    }

    @Test
    public void checkClassNameConversions() {
        assertThat(ConnectorFactory.convertClassNameToServiceName("stratuslab"), equalTo("stratuslab"));
        assertThat(ConnectorFactory
                        .convertClassNameToServiceName("com.sixsq.slipstream.connector.stratuslab.StratusLabConnector"),
                equalTo("stratuslab"));
        assertThat(ConnectorFactory.convertClassNameToServiceName("stratuslab.alpha"), equalTo("stratuslab"));
        assertThat(ConnectorFactory.convertClassNameToServiceName("com.sixsq.slipstream.connector.aws.Ec2Connector"),
                equalTo("ec2"));
        assertThat(ConnectorFactory.convertClassNameToServiceName("aws.Ec2Connector"), equalTo("ec2"));

    }

    @Test
    public void checkConnectorNames() throws Exception {

        CommonTestUtil.setCloudConnector("Local");
        List<String> names = ConnectorFactory.getCloudServiceNamesList();

        assertTrue(names.contains("local"));
    }

    @Test
    public void checkCaseInsensitivityOfConnectorNames() throws Exception {

        CommonTestUtil.setCloudConnector("LOCAL");
        List<String> names = ConnectorFactory.getCloudServiceNamesList();

        assertTrue(names.contains("local"));
    }

    @Test
    public void checkConnectorClassNames() throws Exception {

        CommonTestUtil.setCloudConnector("com.sixsq.slipstream.connector.local.LocalConnector");
        List<String> names = ConnectorFactory.getCloudServiceNamesList();

        assertTrue(names.contains("local"));
    }

    @Test
    public void checkConnectorNamesWithAliases() throws Exception {

        CommonTestUtil.setCloudConnector(
                "SL:com.sixsq.slipstream.connector.local.LocalConnector," + "SL2:local, " +
                        "local");
        List<String> names = ConnectorFactory.getCloudServiceNamesList();

        assertTrue(names.contains("SL"));
        assertTrue(names.contains("SL2"));
        assertTrue(names.contains("local"));
    }

    @Test
    public void checkConnectorNamesWithAliasesAndSpaces() throws Exception {

        CommonTestUtil.setCloudConnector(
                " SL : com.sixsq.slipstream.connector.local.LocalConnector , " + " SL2:local , " +
                        " local ");
        List<String> names = ConnectorFactory.getCloudServiceNamesList();

        assertTrue(names.contains("SL"));
        assertTrue(names.contains("SL2"));
        assertTrue(names.contains("local"));
    }

    @Test
    public void checkConnectorsWithAliasesAndSpaces() throws Exception {

        CommonTestUtil.setCloudConnector(
                " SL : com.sixsq.slipstream.connector.local.LocalConnector , " + " SL2:local , " +
                        " local ");

        assertThat(ConnectorFactory.getConnector("SL"), notNullValue());
        assertThat(ConnectorFactory.getConnector("SL2"), notNullValue());
        assertThat(ConnectorFactory.getConnector("local"), notNullValue());

        Map<String, Connector> connectors = ConnectorFactory.getConnectors();
        assertEquals(connectors.get("openstack"), null);
    }

    @Test
    public void checkConnectorInstanceAndCloudServiceNames() throws Exception {

        CommonTestUtil.setCloudConnector(
                " SL : com.sixsq.slipstream.connector.local.LocalConnector , " + " SL2:local , " +
                        " local ");

        String[] instanceNames = new String[]{"SL", "SL2","local"};
        for (String name : instanceNames) {
            Connector connector = ConnectorFactory.getConnector(name);
            assertThat(connector, notNullValue());

            String instanceName = connector.getConnectorInstanceName();
            assertEquals(name, instanceName);
        }

        Map<String, String> cloudServiceNames = new HashMap<String, String>();
        cloudServiceNames.put("SL", "local");
        cloudServiceNames.put("SL2", "local");
        cloudServiceNames.put("local", "local");

        for (String name : cloudServiceNames.keySet()) {
            Connector connector = ConnectorFactory.getConnector(name);
            assertThat(connector, notNullValue());

            String cloudServiceName = connector.getCloudServiceName();
            assertEquals(cloudServiceNames.get(name), cloudServiceName);
        }
    }

    @Test
    public void checkListAndArrayAreIdentical() throws Exception {
        CommonTestUtil.setCloudConnector("local");

        List<String> names = ConnectorFactory.getCloudServiceNamesList();
        String[] namesArray = ConnectorFactory.getCloudServiceNames();

        Set<String> namesSet = new HashSet<String>(names);
        Set<String> namesArraySet = new HashSet<String>();
        for (String name : namesArray) {
            namesArraySet.add(name);
        }
        assertTrue(namesSet.equals(namesArraySet));
    }
}
