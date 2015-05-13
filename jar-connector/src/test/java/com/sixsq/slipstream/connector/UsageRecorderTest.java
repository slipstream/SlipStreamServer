package com.sixsq.slipstream.connector;

import org.junit.Test;

public class UsageRecorderTest {
    
    @Test
    public void insertStart(){
        UsageRecorder.insertStart("123456", "joe", "aws", UsageRecorder.createVmMetrics());
        UsageRecorder.insertEnd("123456", "joe", "aws");
    }
    
}
