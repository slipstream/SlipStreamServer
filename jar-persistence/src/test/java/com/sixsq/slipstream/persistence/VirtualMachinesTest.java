package com.sixsq.slipstream.persistence;


import org.junit.Assert;
import org.junit.Test;

public class VirtualMachinesTest {

    public static String jsonVirtualMachines() {
        return "{\n" +
                "    \"count\": 1,\n" +
                "    \"acl\": {\n" +
                "        \"owner\": {\n" +
                "            \"principal\": \"ADMIN\",\n" +
                "            \"type\": \"ROLE\"\n" +
                "        },\n" +
                "        \"rules\": [\n" +
                "            {\n" +
                "                \"principal\": \"ADMIN\",\n" +
                "                \"type\": \"ROLE\",\n" +
                "                \"right\": \"MODIFY\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"principal\": \"USER\",\n" +
                "                \"type\": \"ROLE\",\n" +
                "                \"right\": \"VIEW\"\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"resourceURI\": \"http://sixsq.com/slipstream/1/VirtualMachineCollection\",\n" +
                "    \"id\": \"virtual-machine\",\n" +
                "    \"operations\": [\n" +
                "        {\n" +
                "            \"rel\": \"add\",\n" +
                "            \"href\": \"virtual-machine\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"virtualMachines\": [\n" +
                "        {\n" +
                "            \"updated\": \"2017-08-30T08:23:22.903Z\",\n" +
                "            \"credential\": {\n" +
                "                \"href\": \"connector/0123-4567-8912\"\n" +
                "            },\n" +
                "            \"created\": \"2017-08-30T08:23:22.903Z\",\n" +
                "            \"state\": \"Running\",\n" +
                "            \"instanceID\": \"aaa-bb-123\",\n" +
                "            \"id\": \"virtual-machine/6e1bca45-82bd-4fb6-8dff-297970797ea7\",\n" +
                "            \"acl\": {\n" +
                "                \"owner\": {\n" +
                "                    \"type\": \"USER\",\n" +
                "                    \"principal\": \"ADMIN\"\n" +
                "                },\n" +
                "                \"rules\": [\n" +
                "                    {\n" +
                "                        \"right\": \"ALL\",\n" +
                "                        \"type\": \"USER\",\n" +
                "                        \"principal\": \"ADMIN\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"ROLE\",\n" +
                "                        \"principal\": \"ADMIN\",\n" +
                "                        \"right\": \"ALL\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            \"operations\": [\n" +
                "                {\n" +
                "                    \"rel\": \"edit\",\n" +
                "                    \"href\": \"virtual-machine/6e1bca45-82bd-4fb6-8dff-297970797ea7\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"rel\": \"delete\",\n" +
                "                    \"href\": \"virtual-machine/6e1bca45-82bd-4fb6-8dff-297970797ea7\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"resourceURI\": \"http://sixsq.com/slipstream/1/VirtualMachine\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";


    }

    @Test
    public void parseVMRecords() {
        String jsonRecords = VirtualMachinesTest.jsonVirtualMachines();
        VirtualMachines vms = VirtualMachines.fromJson(jsonRecords);
        Assert.assertEquals(1, vms.getVirtualMachines().size());

        VirtualMachine vm = vms.getVirtualMachines().get(0);

        Assert.assertEquals("aaa-bb-123", vm.getInstanceID());

    }

}
