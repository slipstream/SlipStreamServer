package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.persistence.VirtualMachine;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.util.SscljProxy;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

public class VirtualMachineHandlerTest {

    @BeforeClass
    public static void setupClass() {
        SscljProxy.muteForTests();
    }

    @Test
    public void addVMTest(){

        String instanceID = UUID.randomUUID().toString();
        Vm vm = new Vm(instanceID, "0123-4567-8912", "Running", "user", true);

        VirtualMachineHandler.addVM(vm);
    }

    @Test
    public void removeVMTest(){
        String instanceID = UUID.randomUUID().toString();
        Vm vm = new Vm (instanceID, "0123-4567-8912", "state", "Running", true);

        VirtualMachineHandler.addVM(vm);
        VirtualMachineHandler.removeVM(vm);
    }

    @Test
    public void updateVMTest(){

        String instanceID = UUID.randomUUID().toString();
        String cloud = "aCloudName";
        Vm vm = new Vm (instanceID, cloud, "Running", "user", true);
        VirtualMachineHandler.addVM(vm);

        String newState = "Stopped";
        vm.setState(newState);

        VirtualMachineHandler.updateVM(vm);

        VirtualMachine virtualMachine = VirtualMachineHandler.loadVirtualMachine( cloud, instanceID );
        if (virtualMachine != null) {
            Assert.assertEquals(newState, virtualMachine.getState());
        }

    }
}
