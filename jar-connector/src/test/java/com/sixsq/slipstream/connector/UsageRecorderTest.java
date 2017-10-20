package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.persistence.Vm;
import org.junit.Ignore;
import org.junit.Test;

public class UsageRecorderTest extends ConnectorTestBase {

    @Test
    public void insertStart(){
        Vm vm = new Vm("instance-id", "cloud", "running", "user", true);
        for (int i=0; i<100; i++) {
            UsageRecorder.insertStart("123456"+i, "joe", "aws", UsageRecorder.createVmMetrics(vm));
            UsageRecorder.insertEnd("123456"+i, "joe", "aws", UsageRecorder.createVmMetrics(vm));
        }
    }
    
}
