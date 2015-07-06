package com.sixsq.slipstream.connector;

import org.junit.Ignore;
import org.junit.Test;

public class UsageRecorderTest {

    // functional test, ignored in purpose, as there is a dependency to external service.
    @Test
    @Ignore
    public void insertStart(){
        for (int i=0; i<100; i++) {
            UsageRecorder.insertStart("123456"+i, "joe", "aws", UsageRecorder.createVmMetrics());
            UsageRecorder.insertEnd("123456"+i, "joe", "aws");
        }
    }
    
}
