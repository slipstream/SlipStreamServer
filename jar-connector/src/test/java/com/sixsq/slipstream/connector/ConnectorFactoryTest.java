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

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ConnectorFactoryTest {

    @Test
    public void splitConnectorClassNames() {
        assertThat(ConnectorFactory.splitConnectorClassNames(null), is(new String[0]));
    }

    @Test
    public void checkClassNameConversions() {
        assertThat(ConnectorFactory.convertClassNameToServiceName("stratuslab"), equalTo("stratuslab"));
        assertThat(ConnectorFactory
                .convertClassNameToServiceName("com.sixsq.slipstream.connector.stratuslab.StratusLabConnector"),
                equalTo("stratuslab"));
        assertThat(ConnectorFactory.convertClassNameToServiceName("stratuslab.alpha"), equalTo("stratuslab"));
    }
}
