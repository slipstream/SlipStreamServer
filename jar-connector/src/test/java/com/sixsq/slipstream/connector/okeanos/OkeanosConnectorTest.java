package com.sixsq.slipstream.connector.okeanos;

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

import com.sixsq.slipstream.connector.stratuslab.StratusLabConnector;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class OkeanosConnectorTest {

    @Test
    public void checkConstructors() {
        assertThat(new OkeanosConnector().getConnectorInstanceName(), notNullValue());
        assertThat(new OkeanosConnector(null).getConnectorInstanceName(), notNullValue());
        assertThat(new OkeanosConnector("MyID").getConnectorInstanceName(), notNullValue());
    }
}
