package com.sixsq.slipstream.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.ValidationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.sixsq.slipstream.util.SscljProxy.parseJson;
import static org.junit.Assert.*;

public class ServiceOffersUtilTest {

    public static String EMPTY_JSON = "{}";

    @BeforeClass
    public static void setupClass() {
        SscljProxy.muteForTests();
    }

    @AfterClass
    public static void teardownClass() {
        SscljProxy.unmuteForTests();
    }

    @Test
    public void unknownServiceOffer() {
        JsonObject obj = ServiceOffersUtil.getServiceOffer("UnknownServiceOfferId");
        assertNotNull(obj);
        assertTrue(obj.toString().equalsIgnoreCase(EMPTY_JSON));
    }


    @Test
    public void nullServiceOffer() {
        JsonObject obj = ServiceOffersUtil.getServiceOffer(null);
        assertNotNull(obj);
        assertTrue(obj.toString().equalsIgnoreCase(EMPTY_JSON));
    }

    @Test
    public void parseNull() {
        JsonObject obj = parseJson(null);
    }

    @Test(expected = IllegalStateException.class)
    public void parseEmptyString() {
        JsonObject obj = parseJson("");
    }


    @Test
    public void parseEmptyJson() {
        JsonObject obj = parseJson(EMPTY_JSON);
        assertNotNull(obj);
        assertTrue(obj.toString().equalsIgnoreCase(EMPTY_JSON));
    }

    @Test
    public void serviceOfferAttributeName() {
        JsonObject emptyServiceOffer = new JsonObject();
        String anAttributeName = "myAttribute";
        String valueName = "myValue";
        JsonObject aServiceOffer = (JsonObject) new JsonParser().parse("{\"id\":\"my-service-offer-id\", " + anAttributeName +
                ":"+valueName+"}");



        //Null offer

        JsonElement obj ;
        try {
            obj = ServiceOffersUtil.getServiceOfferAttribute(null, anAttributeName);

            fail();
        } catch (ValidationException e) {
            assertTrue(true);
        }

        try {
            obj = ServiceOffersUtil.getServiceOfferAttribute(null, null);

            fail();
        } catch (ValidationException e) {
            assertTrue(true);
        }


        //Empty offer
        try {
            obj = ServiceOffersUtil.getServiceOfferAttribute(emptyServiceOffer, anAttributeName);

            fail();
        } catch (ValidationException e) {
            assertTrue(true);
        }
        try {
            obj = ServiceOffersUtil.getServiceOfferAttribute(emptyServiceOffer, null);

            fail();
        } catch (ValidationException e) {
            assertTrue(true);
        }

        //Actual offer
        try {
            obj = ServiceOffersUtil.getServiceOfferAttribute(aServiceOffer, anAttributeName);

            assertNotNull(obj);
            assertTrue("Expected : ["+ valueName +"] but got ["+ obj.getAsString()+"]", obj.getAsString().equalsIgnoreCase(valueName));
        } catch (ValidationException e) {
            fail();
        }
        try {
            obj = ServiceOffersUtil.getServiceOfferAttribute(aServiceOffer, null);

            fail();
        } catch (ValidationException e) {
            assertTrue(true);
        }

        {
            obj = ServiceOffersUtil.getServiceOfferAttributeOrNull(aServiceOffer, null);

            assertNull(obj);
        }

    }
}
