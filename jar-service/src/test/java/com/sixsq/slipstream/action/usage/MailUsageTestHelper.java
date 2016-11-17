package com.sixsq.slipstream.action.usage;


public class MailUsageTestHelper {

    public static String jsonUsages() {
        return
                "{\n" +
                        "  \"usages\" : [ {\n" +
                        "    \"usageSummary\" : {\n" +
                        "      \"RAM\" : {\n" +
                        "        \"unit-minutes\" : 1916151.47\n" +
                        "      },\n" +
                        "      \"instance-type.Medium\" : {\n" +
                        "        \"unit-minutes\" : 84.20\n" +
                        "      },\n" +
                        "      \"vm\" : {\n" +
                        "        \"unit-minutes\" : 1688.23\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"end-timestamp\" : \"2015-06-16T00:00:00.000Z\",\n" +
                        "    \"start-timestamp\" : \"2015-06-15T00:00:00.000Z\",\n" +
                        "    \"cloud\" : \"cloud-0\",\n" +
                        "    \"user\" : \"joe\",\n" +
                        "    \"acl\" : {\n" +
                        "      \"owner\" : {\n" +
                        "        \"type\" : \"USER\",\n" +
                        "        \"principal\" : \"joe\"\n" +
                        "      },\n" +
                        "      \"rules\" : [ {\n" +
                        "        \"type\" : \"USER\",\n" +
                        "        \"principal\" : \"joe\",\n" +
                        "        \"right\" : \"ALL\"\n" +
                        "      }, {\n" +
                        "        \"type\" : \"ROLE\",\n" +
                        "        \"principal\" : \"cloud-0\",\n" +
                        "        \"right\" : \"ALL\"\n" +
                        "      } ]\n" +
                        "    },\n" +
                        "    \"id\" : \"Usage/0954a8f0-7ccb-4386-9797-308defa76ad7\"\n" +
                        "  }, {\n" +
                        "    \"usageSummary\" : {\n" +
                        "      \"ram\" : {\n" +
                        "        \"unit-minutes\" : 47185920.0\n" +
                        "      },\n" +
                        "      \"instance-type.Huge\" : {\n" +
                        "        \"unit-minutes\" : 1440.0\n" +
                        "      },\n" +
                        "      \"vm\" : {\n" +
                        "        \"unit-minutes\" : 1440.0\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"end-timestamp\" : \"2015-06-16T00:00:00.000Z\",\n" +
                        "    \"start-timestamp\" : \"2015-06-15T00:00:00.000Z\",\n" +
                        "    \"cloud\" : \"cloud-3\",\n" +
                        "    \"user\" : \"joe\",\n" +
                        "    \"acl\" : {\n" +
                        "      \"owner\" : {\n" +
                        "        \"type\" : \"USER\",\n" +
                        "        \"principal\" : \"joe\"\n" +
                        "      },\n" +
                        "      \"rules\" : [ {\n" +
                        "        \"type\" : \"USER\",\n" +
                        "        \"principal\" : \"joe\",\n" +
                        "        \"right\" : \"ALL\"\n" +
                        "      }, {\n" +
                        "        \"type\" : \"ROLE\",\n" +
                        "        \"principal\" : \"cloud-3\",\n" +
                        "        \"right\" : \"ALL\"\n" +
                        "      } ]\n" +
                        "    },\n" +
                        "    \"id\" : \"Usage/2e6f7519-8c2d-4924-b492-29371c032c83\"\n" +
                        "  }],\n" +
                        "  \"operations\" : [ {\n" +
                        "    \"rel\" : \"http://sixsq.com/slipstream/1/Action/add\",\n" +
                        "    \"href\" : \"Usage\"\n" +
                        "  } ],\n" +
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
                        "  \"count\" : 2\n" +
                        "}";
    }

}
