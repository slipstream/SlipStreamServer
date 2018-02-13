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

import com.google.gson.Gson;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.SlipStreamException;
import com.sixsq.slipstream.factory.RunFactory;
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.ModuleParameter;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RunType;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.ssclj.app.CIMITestServer;
import com.sixsq.slipstream.util.SscljProxy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class CliConnectorBaseTest {

    @BeforeClass
    public static void setupClass() {
        CIMITestServer.start();
    }

    @AfterClass
    public static void teardownClass() {
        CIMITestServer.stop();
    }

    private void parseRunInstanceResultExpectException(String output) {
        boolean exception = false;
        try {
            CliConnectorBase.parseRunInstanceResult(output);
        } catch (SlipStreamClientException e) {
            exception = true;
        }
        assertTrue("SlipStreamClientException should have been returned an exception for: " + output, exception);
    }

    @Test
    public void parseRunInstanceResultTest() throws Exception {
        String id = "3dd40aee-c42e-4eb6-a973-0f6f8fc71b08";
        String ip = "127.0.0.1";

        ArrayList<String> outputs = new ArrayList<>();
        outputs.add(id + "," + ip);
        outputs.add(id + ",");
        outputs.add("\n" + id + "," + ip);
        outputs.add("\n" + id + "," + ip + "\n");
        outputs.add("\n" + id + "," + ip + "\n" + id + "," + ip);
        outputs.add(id + ",\n" + id + "," + ip);
        outputs.add(id + ",\n" + id + "," + ip + "\n");
        outputs.add("ERROR: 1+1 not equal to 2 !!!\n" + id);
        outputs.add("\n" + id + "," + ip + "\n," + ip);
        outputs.add("\n" + id + ",\n," + ip);
        outputs.add("\n" + id + "," + ip + "\n,");

        for (String output : outputs) {
            String[] result = CliConnectorBase.parseRunInstanceResult(output);
            String resId = result[0];
            String resIp = result[1];
            String message = "for output: " + output;

            if (output.contains(id)) assertEquals(message, id, resId);
            if (output.contains(ip)) assertEquals(message, ip, resIp);
        }

        ArrayList<String> outputsWithException = new ArrayList<>();
        outputsWithException.add(",");
        outputsWithException.add("");
        outputsWithException.add("," + ip);

        for (String output : outputsWithException) {
            parseRunInstanceResultExpectException(output);
        }

    }

    @Test
    public void credentialTemplateTest() {
        Gson gson = new Gson();
        HashMap<String, Object> credTmpl = CliConnectorBase.getCredentialTemplateApiKeySecret(0);
        assertTrue(credTmpl.containsKey("credentialTemplate"));
        HashMap<String, Object> tmpl = (HashMap<String, Object>) credTmpl.get("credentialTemplate");
        assertEquals(0, tmpl.get("ttl"));
        HashMap credTmplConverted = gson.fromJson(gson.toJson(credTmpl), HashMap.class);
        assertTrue(credTmplConverted.containsKey("credentialTemplate"));
        Map<String, Object> tmplConverted = (Map<String, Object>) credTmplConverted.get("credentialTemplate");
        assertEquals(0, Math.round((Double) tmplConverted.get("ttl")));
    }

    @Test
    public void generateApiKeySecretPairTest() {
        Map<String, String> keySecretPair = CliConnectorBase.generateApiKeySecretPair("testuser", "1-2-3-4-5");
        assertTrue(keySecretPair.containsKey("key"));
        assertTrue(keySecretPair.get("key").startsWith("credential/"));
        assertTrue(keySecretPair.containsKey("secret"));
        assertTrue(!keySecretPair.get("secret").isEmpty());
    }

    @Test
    public void runApiKeyLifecycleTest() throws SlipStreamException {

        Map<String, String> environment = new HashMap();
        String userName = "user";

        Run run = createAndStoreRun(userName);

        assertTrue(run.getRuntimeParameters().containsKey(RuntimeParameter.GLOBAL_RUN_APIKEY_KEY));

        CliConnectorBase.genAndSetRunApiKey(run, userName, environment);

        // Set in environment for CLI.
        assertTrue(environment.containsKey("SLIPSTREAM_API_KEY"));
        assertTrue(environment.containsKey("SLIPSTREAM_API_SECRET"));
        assertTrue(!environment.get("SLIPSTREAM_API_KEY").isEmpty());

        // Persisted as RTP.
        run = Run.loadRunWithRuntimeParameters(run.getUuid());
        String key = run.getRuntimeParameterValue(RuntimeParameter.GLOBAL_RUN_APIKEY_KEY);
        assertThat(key, is(environment.get("SLIPSTREAM_API_KEY")));

        // Persisted as CIMI resource.
        Response resp = SscljProxy.get(SscljProxy.BASE_RESOURCE + key, userName + " USER");
        assertTrue(resp.getStatus().isSuccess());
        Gson gson = new Gson();
        HashMap apiKey = gson.fromJson(resp.getEntityAsText(), HashMap.class);
        assertThat(key, is(apiKey.get("id")));

        // Delete API key/secret resource.
        CliConnectorBase.deleteApiKeySecret(run, new User(userName), Logger.getLogger("test"));
        resp = SscljProxy.get(SscljProxy.BASE_RESOURCE + key, userName + " USER");
        assertTrue(resp.getStatus().isError());
    }

    private Run createAndStoreRun(String userName) throws SlipStreamClientException {
        String cloudServiceName = "testcloud";
        ImageModule image = new ImageModule("test/image");
        image.setImageId("123", cloudServiceName);
        image.setIsBase(true);
        image.store();

        HashMap<String, List<Parameter<?>>> userChoices = new HashMap<>();

        Parameter<?> cloudService = new ModuleParameter(RuntimeParameter.CLOUD_SERVICE_NAME);
        cloudService.setValue(cloudServiceName);

        userChoices.put(Run.MACHINE_NAME, new ArrayList<>());
        userChoices.get(Run.MACHINE_NAME).add(cloudService);

        Run run = RunFactory.getRun(image, RunType.Run, new User(userName), userChoices);
        return run.store();
    }
}
