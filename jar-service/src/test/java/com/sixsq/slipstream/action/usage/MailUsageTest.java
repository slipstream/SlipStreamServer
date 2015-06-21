package com.sixsq.slipstream.action.usage;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

public class MailUsageTest {

    @Test
    @Ignore
    public void htmlFormatting() {

        List<UsageSummary> usageSummaries = UsageSummaries.fromJson(MailUsageTestHelper.jsonUsages()).usages;

        Calendar start = Calendar.getInstance();
        start.set(2015, 3, 16, 0, 0);
        Calendar end = Calendar.getInstance();
        end.set(2015, 3, 17, 0, 0);

        MailUsage mailUsage = new MailUsage(start.getTime(), end.getTime(), "stef", "st@sixsq.com", usageSummaries);

        System.out.println(mailUsage.body());
        Assert.assertNotNull(mailUsage.body());
    }
}
