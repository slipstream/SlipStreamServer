package com.sixsq.slipstream.action;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
public class DailyUsageSenderTest {

    @Test
    public void encodeParameters() {
        Assert.assertEquals("", DailyUsageSender.encodeQueryParameters(""));
        Assert.assertEquals("a=1", DailyUsageSender.encodeQueryParameters("a=1"));
        Assert.assertEquals("a=1&b=2", DailyUsageSender.encodeQueryParameters("a=1&b=2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cimiQueryStringUsageRequiresUserNamesNotNull() {
        DailyUsageSender.cimiQueryStringUsage(null, "2015-12-31", "2016-01-01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cimiQueryStringUsageRequiresUserNamesNotEmpty() {
        DailyUsageSender.cimiQueryStringUsage(new HashSet<String>(), "2015-12-31", "2016-01-01");
    }

    @Test
    public void cimiQueryStringUsage() {
        Set<String> userNames = new HashSet<String>(Arrays.asList("joe"));
        Assert.assertEquals(
                "%24filter=start_timestamp%3D2015-12-31+and+end_timestamp%3D2016-01-01+and+%28user%3D%27joe%27%29",
                DailyUsageSender.cimiQueryStringUsage(userNames, "2015-12-31", "2016-01-01"));

        userNames = new HashSet<String>(Arrays.asList("joe", "jack"));
        Assert.assertEquals(
                "%24filter=start_timestamp%3D2015-12-31+and+end_timestamp%3D2016-01-01+and+%28user%3D%27joe%27+or+user%3D%27jack%27%29",
                DailyUsageSender.cimiQueryStringUsage(userNames, "2015-12-31", "2016-01-01"));
    }

    @Test
    @Ignore
    public void dateY() {
        Set<String> userNames = new HashSet<String>(Arrays.asList("joe"));
        System.out.println(DailyUsageSender.yesterday());
        System.out.println(DailyUsageSender.formatDate(DailyUsageSender.yesterday()));
        System.out.println(DailyUsageSender.cimiQueryStringUsageYesterday(userNames));
    }
}
