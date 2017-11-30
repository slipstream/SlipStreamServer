package com.sixsq.slipstream.persistence;


import org.junit.Assert;
import org.junit.Test;

public class VmMappingServiceOfferTest {

    private final String cloud = "exoscale-ch-gva";
    private final String instanceId = "aaa-bb-123";
    private final String runUuid = "run/b836e665-74df-4800-89dc-c746c335a6a9";
    private final String owner = "user/janedoe";
    private final String serviceOffer = "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3";

    private void jsonRoundtripAssert(VmMappingServiceOffer vmMappingServiceOffer) {
        String json = vmMappingServiceOffer.toJson();
        VmMappingServiceOffer newVmMappingServiceOffer = VmMappingServiceOffer.fromJson(json);

        Assert.assertEquals(vmMappingServiceOffer.toString(), newVmMappingServiceOffer.toString());
    }

    @Test
    public void parseVmMappingServiceOffer() {

        VmMappingServiceOffer vmMappingServiceOffer;

        vmMappingServiceOffer = new VmMappingServiceOffer(serviceOffer);
        jsonRoundtripAssert(vmMappingServiceOffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullHref() {
        new VmMappingServiceOffer(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlankHref() {
        new VmMappingServiceOffer("");
    }

    @Test
    public void testJsonIdentifierMapping() {
        VmMappingServiceOffer vmMappingServiceOffer;
        String json;

        vmMappingServiceOffer = new VmMappingServiceOffer(serviceOffer);
        json = vmMappingServiceOffer.toJson();

        Assert.assertTrue(json.contains("\"href\""));
    }
}
