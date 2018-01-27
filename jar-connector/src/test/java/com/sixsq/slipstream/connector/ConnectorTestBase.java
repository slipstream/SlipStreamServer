package com.sixsq.slipstream.connector;

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

import com.sixsq.slipstream.es.CljElasticsearchHelper;
import com.sixsq.slipstream.ssclj.app.CIMITestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class ConnectorTestBase {

    @BeforeClass
    public static void setupClass() {
        setupBackend();
    }

    public static void setupBackend() {
        CIMITestServer.start();
        CljElasticsearchHelper.initTestDb();
    }

    @AfterClass
    public static void teardownClass() {
        teardownBackend();
    }

    public static void teardownBackend() {
        CIMITestServer.stop();
    }
}
