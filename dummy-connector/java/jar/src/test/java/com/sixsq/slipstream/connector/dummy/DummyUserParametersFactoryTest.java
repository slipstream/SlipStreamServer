package com.sixsq.slipstream.connector.dummy;

/*
 * +=================================================================+
 * SlipStream Connector Dummy
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

import java.util.Map;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.util.CloudCredDefTestBase;
import org.junit.Test;
import static org.junit.Assert.*;

public class DummyUserParametersFactoryTest extends CloudCredDefTestBase {

    @Test
    public void paramsDescriptionLoadedTest() throws ValidationException {
        UserParametersFactoryBase fb = new DummyUserParametersFactory("foo-bar-baz");
        Map<String, UserParameter> userCloudParams = fb.getParameters();
        assertNotNull(userCloudParams);
        assertTrue(userCloudParams.size() >= 2);
        for (UserParameter p: userCloudParams.values()) {
            assertNotNull(p.getDescription());
        }
    }
}
