package com.sixsq.slipstream.action.usage;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

public class MailUsageTest {

    @Test
    public void htmlFormatting() {

        List<UsageSummary> usageSummaries = UsageSummaries.fromJson(MailUsageTestHelper.jsonUsages()).usages;

        Calendar start = Calendar.getInstance();
        start.set(2015, 3, 16, 0, 0);
        Calendar end = Calendar.getInstance();
        end.set(2015, 3, 17, 0, 0);

        MailUsage mailUsage = new MailUsage(start.getTime(), end.getTime(), "stef", "st@sixsq.com", usageSummaries) {
            @Override
            protected String baseUrl() {
                return "https://nuv.la";
            }

            @Override
            protected String currentVersion() {
                return "test";
            }
        };

        // System.out.println(mailUsage.body());

        Assert.assertNotNull(mailUsage.body());

        Assert.assertTrue(mailUsage.body().indexOf("cloud-0") < mailUsage.body().indexOf("cloud-3"));

        assertBodyContains(mailUsage, "Usage Report for stef on nuv.la");
        assertBodyContains(mailUsage, "Daily usage for Apr 16, 2015");
        assertBodyContains(mailUsage, "<tr><td>vm</td><td>4720.00</td></tr>");
        assertBodyContains(mailUsage, "<tr><td>DISK</td><td>185400.00</td></tr>");
        assertBodyContains(mailUsage, "<tr><td>RAM</td><td>5344.00</td></tr>");

        assertBodyContains(mailUsage, "<tr><td>vm</td><td>2882.00</td></tr>");
        assertBodyContains(mailUsage, "<tr><td>DISK</td><td>216600.00</td></tr>");
        assertBodyContains(mailUsage, "<tr><td>RAM</td><td>28032.00</td></tr>");

        assertBodyContains(mailUsage, "href=\"https://nuv.la/api/usage?%24filter=start_timestamp%3D2015-04-16+and+end_timestamp%3D2015-04-17+and+%28user%3D%27stef%27%29\"");
    }

    private void assertBodyContains(MailUsage mailUsage, String expected) {
        Assert.assertTrue(mailUsage.body().contains(expected));
    }

}
