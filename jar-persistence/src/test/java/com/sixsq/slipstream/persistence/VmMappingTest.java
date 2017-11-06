package com.sixsq.slipstream.persistence;


import org.junit.Assert;
import org.junit.Test;

public class VmMappingTest {

    private final String cloud = "exoscale-ch-gva";
    private final String instanceId = "aaa-bb-123";
    private final String runUuid = "run/b836e665-74df-4800-89dc-c746c335a6a9";
    private final String owner = "user/janedoe";
    private final String serviceOffer = "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3";

    private void jsonRoundtripAssert(VmMapping vmMapping) {
        String json = vmMapping.toJson();
        VmMapping newVmMapping = VmMapping.fromJson(json);

        Assert.assertEquals(vmMapping.toString(), newVmMapping.toString());
    }

    @Test
    public void parseVmMapping() {

        VmMapping vmMapping;

        vmMapping = new VmMapping(cloud, instanceId, runUuid, owner, serviceOffer);
        jsonRoundtripAssert(vmMapping);

        vmMapping = new VmMapping(cloud, instanceId, runUuid, owner, null);
        jsonRoundtripAssert(vmMapping);

        vmMapping = new VmMapping(cloud, instanceId, runUuid, owner, "");
        jsonRoundtripAssert(vmMapping);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadCloud() {
        new VmMapping(null, instanceId, runUuid, owner, serviceOffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadCloudBlank() {
        new VmMapping("", instanceId, runUuid, owner, serviceOffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadInstanceId() {
        new VmMapping(cloud, null, runUuid, owner, serviceOffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadInstanceIdBlank() {
        new VmMapping(cloud, "", runUuid, owner, serviceOffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadRunUuid() {
        new VmMapping(cloud, instanceId, null, owner, serviceOffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadRunUuidBlank() {
        new VmMapping(cloud, instanceId, "", owner, serviceOffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadOwner() {
        new VmMapping(cloud, instanceId, runUuid, null, serviceOffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadOwnerBlank() {
        new VmMapping(cloud, instanceId, runUuid, "", serviceOffer);
    }

    @Test
    public void testJsonIdentifierMapping() {
        VmMapping vmMapping;
        String json;

        vmMapping = new VmMapping(cloud, instanceId, runUuid, owner, serviceOffer);
        json = vmMapping.toJson();

        Assert.assertTrue(json.contains("\"cloud\""));
        Assert.assertTrue(json.contains("\"instanceID\""));
        Assert.assertTrue(json.contains("\"runUUID\""));
        Assert.assertTrue(json.contains("\"owner\""));
        Assert.assertTrue(json.contains("\"serviceOffer\""));
        Assert.assertTrue(json.contains("\"href\""));

        vmMapping = new VmMapping(cloud, instanceId, runUuid, owner, null);
        json = vmMapping.toJson();

        Assert.assertTrue(json.contains("\"cloud\""));
        Assert.assertTrue(json.contains("\"instanceID\""));
        Assert.assertTrue(json.contains("\"runUUID\""));
        Assert.assertTrue(json.contains("\"owner\""));
        Assert.assertFalse(json.contains("\"serviceOffer\""));
        Assert.assertFalse(json.contains("\"href\""));
    }
}
