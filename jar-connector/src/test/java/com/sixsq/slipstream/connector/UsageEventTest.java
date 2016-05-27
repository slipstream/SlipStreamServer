package com.sixsq.slipstream.connector;


import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class UsageEventTest {

    @Test
    public void testUsageRecordToJson() {

        TypePrincipal owner = new TypePrincipal(TypePrincipal.PrincipalType.USER, "joe");
        List<TypePrincipalRight> rules = new ArrayList<TypePrincipalRight>();
        rules.add(new TypePrincipalRight(TypePrincipal.PrincipalType.USER, "joe", TypePrincipalRight.Right.ALL));

        ACL acl = new ACL(owner, rules);

        List<UsageMetric> metrics = Arrays.asList(
                new UsageMetric("cpu", "1.0"),
                new UsageMetric("disk", "100.0"));
        

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Geneva"));
        c.set(Calendar.YEAR, 2015);
        c.set(Calendar.MONTH, 1);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 7);
        c.set(Calendar.MINUTE, 12);
        c.set(Calendar.SECOND, 45);
        c.set(Calendar.MILLISECOND, 123);

        Date start_timestamp = c.getTime();

        UsageEvent ur = new UsageEvent(acl, "joe", "aws", "aws:123",
                start_timestamp, null, metrics);

        String expectedJson = "{\n  \"acl\": {\n" +
                "    \"owner\": {\n" +
                "      \"type\": \"USER\",\n" +
                "      \"principal\": \"joe\"\n" +
                "    },\n" +
                "    \"rules\": [\n" +
                "      {\n" +
                "        \"right\": \"ALL\",\n" +
                "        \"type\": \"USER\",\n" +
                "        \"principal\": \"joe\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"user\": \"joe\",\n" +
                "  \"cloud\": \"aws\",\n" +
                "  \"start_timestamp\": \"2015-02-01T07:12:45.123Z\",\n" +
                "  \"cloud_vm_instanceid\": \"aws:123\",\n" +
                "  \"metrics\": [\n" +
                "    {\n" +
                "      \"name\": \"cpu\",\n" +
                "      \"value\": \"1.0\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"disk\",\n" +
                "      \"value\": \"100.0\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        Assert.assertEquals(expectedJson, ur.toJson());
    }
}