package com.sixsq.slipstream.connector;


import org.junit.Assert;
import org.junit.Test;

public class CollectorComparisonTest {

    @Test
    public void testFloatComparisons(){
        Float nullFloat = null;

        Assert.assertTrue(Collector.areEquals(nullFloat, nullFloat));

        Assert.assertFalse(Collector.areEquals(null, new Float("3.14")));
        Assert.assertFalse(Collector.areEquals(new Float("3.14"), null));
        Assert.assertTrue(Collector.areEquals(new Float("3.14"), new Float("3.14")));
        Assert.assertFalse(Collector.areEquals(new Float("3.14"), new Float("2.7182828")));

        Assert.assertFalse(Collector.areEquals(new Integer(12), null));
        Assert.assertFalse(Collector.areEquals(null, new Integer(12)));
        Assert.assertTrue(Collector.areEquals(new Integer(12), new Integer(12)));
        Assert.assertFalse(Collector.areEquals(new Integer(12), new Integer(45)));

        Assert.assertFalse(Collector.areEquals("small", null));
        Assert.assertFalse(Collector.areEquals(null, "small"));
        Assert.assertTrue(Collector.areEquals(new String("small"), new String("small")));
        Assert.assertTrue(Collector.areEquals("small", "small"));
        Assert.assertFalse(Collector.areEquals("small", "big"));
    }

}
