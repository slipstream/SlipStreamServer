package com.sixsq.slipstream.action.usage;

import junit.framework.Assert;
import org.junit.Test;

public class UsageSummariesTest {

    @Test
    public void parseEmptyJsonUsages() {
        String jsonUsages =
                "{\n" +
                        "  \"acl\" : {\n" +
                        "    \"rules\" : [ {\n" +
                        "      \"type\" : \"ROLE\",\n" +
                        "      \"right\" : \"VIEW\",\n" +
                        "      \"principal\" : \"ANON\"\n" +
                        "    } ],\n" +
                        "    \"owner\" : {\n" +
                        "      \"type\" : \"ROLE\",\n" +
                        "      \"principal\" : \"ADMIN\"\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"resourceURI\" : \"http://sixsq.com/slipstream/1/UsageCollection\",\n" +
                        "  \"id\" : \"Usage\",\n" +
                        "  \"count\" : 0\n" +
                        "}";
        UsageSummaries usageSummaries = UsageSummaries.fromJson(jsonUsages);
        Assert.assertEquals(0, usageSummaries.usages.size());
    }

    @Test
    public void parseJsonUsages() {

        String jsonUsages = MailUsageTestHelper.jsonUsages();
        UsageSummaries usageSummaries = UsageSummaries.fromJson(jsonUsages);

        Assert.assertEquals(2, usageSummaries.usages.size());

        for (UsageSummary usageSummary : usageSummaries.usages) {
            Assert.assertEquals("2015-06-15T00:00:00.000Z", usageSummary.start_timestamp);
            Assert.assertEquals("2015-06-16T00:00:00.000Z", usageSummary.end_timestamp);
            Assert.assertEquals("joe", usageSummary.user);
        }

        UsageSummary usage1 = usageSummaries.usages.get(0);
        Assert.assertEquals("cloud-0", usage1.cloud);
        Assert.assertEquals(5344.0, usage1.getMetrics().get("RAM"));
        Assert.assertEquals(185400.0, usage1.getMetrics().get("DISK"));
        Assert.assertEquals(4720.0, usage1.getMetrics().get("vm"));

        UsageSummary usage2 = usageSummaries.usages.get(1);
        Assert.assertEquals("cloud-3", usage2.cloud);
        Assert.assertEquals(28032.0, usage2.getMetrics().get("RAM"));
        Assert.assertEquals(216600.0, usage2.getMetrics().get("DISK"));
        Assert.assertEquals(2882.0, usage2.getMetrics().get("vm"));
    }
}

