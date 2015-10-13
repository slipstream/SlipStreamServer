package com.sixsq.slipstream.action.usage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2015 SixSq Sarl (sixsq.com)
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
public class MailUtilsTest {

    @Test
    public void formatValueForVM() {
        assertEquals("0.27 (h)", MailUtils.formatMetricValue("instance-type.Small", 16.133333333333333));
        assertEquals("0.37 (h)", MailUtils.formatMetricValue("instance-type.Huge", 22.48333333333333));
        assertEquals("24.00 (h)", MailUtils.formatMetricValue("instance-type.Huge", 1440.0));
        assertEquals("1.40 (h)", MailUtils.formatMetricValue("instance-type.Medium", 84.2));
        assertEquals("26.09 (h)", MailUtils.formatMetricValue("instance-type.Micro", 1565.416666666667));
        assertEquals("28.14 (h)", MailUtils.formatMetricValue("vm", 1688.2333333333331));
        assertEquals("0.00 (h)", MailUtils.formatMetricValue("vm", 0.0));
    }

    @Test
    public void formatValueForRAM() {
        // 47185920 is the value for Huge instance (32GB RAM) running for a full day
        // 768 is 32 * 24
        assertEquals("768.00 (GBh)", MailUtils.formatMetricValue("ram", 47185920));
        assertEquals("0.00 (GBh)", MailUtils.formatMetricValue("ram", 0));
    }

    @Test
    public void formatValueUnknownUnit() {
        assertEquals("16.13", MailUtils.formatMetricValue("anything", 16.133333333333333));
    }
}
