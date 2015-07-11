package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.persistence.Vm;
import org.junit.Ignore;
import org.junit.Test;

public class UsageRecorderTest {
    
    @Test
    @Ignore
    public void insertStart(){
        Vm vm = new Vm("instance-id", "cloud", "running", "user", true);
        UsageRecorder.insertStart("123456", "joe", "aws", UsageRecorder.createVmMetrics(vm));
        UsageRecorder.insertEnd("123456", "joe", "aws");
    }
    
}
