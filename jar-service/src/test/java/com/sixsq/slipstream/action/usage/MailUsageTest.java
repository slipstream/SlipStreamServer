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

        // System.out.println(mailUsage.body()); // usefull to copypaste in scratch file and see the actual result

        Assert.assertNotNull(mailUsage.body());

        Assert.assertTrue(mailUsage.body().indexOf("cloud-0") < mailUsage.body().indexOf("cloud-3"));

        assertBodyContains(mailUsage, "Usage Report for stef on nuv.la");
        assertBodyContains(mailUsage, "Daily usageSummary for Apr 16, 2015");
        assertBodyContains(mailUsage, "<tr><td style=\"width:50%\">instance-type.Medium</td><td style=\"width:50%\">1.40 (h)</td></tr>");
        assertBodyContains(mailUsage, "<tr><td style=\"width:50%\">vm</td><td style=\"width:50%\">28.14 (h)</td></tr>");
        assertBodyContains(mailUsage, "<tr><td style=\"width:50%\">RAM</td><td style=\"width:50%\">31.19 (GBh)</td></tr>");

        assertBodyContains(mailUsage, "<tr><td style=\"width:50%\">instance-type.Huge</td><td style=\"width:50%\">24.00 (h)</td></tr>");
        assertBodyContains(mailUsage, "<tr><td style=\"width:50%\">vm</td><td style=\"width:50%\">24.00 (h)</td></tr>");
        assertBodyContains(mailUsage, "<tr><td style=\"width:50%\">ram</td><td style=\"width:50%\">768.00 (GBh)</td></tr>");

        assertBodyContains(mailUsage, "href=\"https://nuv.la/api/usageSummary?%24filter=start-timestamp%3D2015-04-16+and+end-timestamp%3D2015-04-17+and+%28user%3D%27stef%27%29\"");
    }

    private void assertBodyContains(MailUsage mailUsage, String expected) {
        Assert.assertTrue(mailUsage.body().contains(expected));
    }

}
