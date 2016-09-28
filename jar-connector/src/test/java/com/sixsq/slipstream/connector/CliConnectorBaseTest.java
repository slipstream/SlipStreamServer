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

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.util.CommonTestUtil;
import com.sixsq.slipstream.util.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class CliConnectorBaseTest {

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
        outputs.add(id+","+ip);
        outputs.add(id+",");
        outputs.add("\n"+id+","+ip);
        outputs.add("\n"+id+","+ip+"\n");
        outputs.add("\n"+id+","+ip+"\n"+id+","+ip);
        outputs.add(id+",\n"+id+","+ip);
        outputs.add(id+",\n"+id+","+ip+"\n");
        outputs.add("ERROR: 1+1 not equal to 2 !!!\n"+id);
        outputs.add("\n"+id+","+ip+"\n,"+ip);
        outputs.add("\n"+id+",\n,"+ip);
        outputs.add("\n"+id+","+ip+"\n,");

        for (String output: outputs) {
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
        outputsWithException.add(","+ip);

        for (String output: outputsWithException) {
            parseRunInstanceResultExpectException(output);
        }

    }

}
