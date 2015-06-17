package com.sixsq.slipstream.action.usage;

import junit.framework.Assert;
import org.junit.Test;

import java.util.List;

public class MailUsageTest {

    @Test
    public void htmlFormatting() {

        List<UsageSummary> usageSummaries = UsageSummaries.fromJson(MailUsageTestHelper.jsonUsages()).usages;
        MailUsage mailUsage = new MailUsage("jun, 17 2015", "stef", "st@sixsq.com", usageSummaries);

        Assert.assertNotNull(mailUsage.body());

    }
}
