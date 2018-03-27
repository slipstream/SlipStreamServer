package com.sixsq.slipstream.configuration;

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

import com.sixsq.slipstream.es.CljElasticsearchHelper;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ServiceConfiguration;
import com.sixsq.slipstream.ssclj.app.CIMITestServer;
import com.sixsq.slipstream.util.SscljProxy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Response;

import java.util.HashMap;

import static org.junit.Assert.fail;


public class ConfigurationTest {

    @BeforeClass
    public static void setupClass() {
        CIMITestServer.start();
        CljElasticsearchHelper.initTestDb();
        Event.muteForTests();
    }

    @AfterClass
    public static void teardownClass() throws ValidationException {
        Configuration.getInstance().reinitialise();
        CIMITestServer.stop();
    }

    @Test
    public void refreshSuccess() throws ValidationException {
        Configuration.refreshRateSec = 1;
        Configuration config = Configuration.getInstance();
        assert 1 == config.refreshRateSec;
        HashMap conf = new HashMap<String, String>();
        String version = "";
        for (int i = 0; i < 3; i++) {
            version = "0." + i;

            conf.put("slipstreamVersion", version);
            Response resp = SscljProxy.put(SscljProxy.BASE_RESOURCE + "configuration/slipstream", "root ADMIN", conf);
            if (SscljProxy.isError(resp)) {
                fail("Failed to update configuration with: " + resp.getEntityAsText());
            }

            try {
                Thread.sleep(Configuration.refreshRateSec * 1000 + 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String paramName = ServiceConfiguration.RequiredParameters.SLIPSTREAM_VERSION.getName();
            String versionBackend = config.getParameters().getParameter(paramName).getValue();
            assert version.equals(versionBackend);
        }
    }
}
