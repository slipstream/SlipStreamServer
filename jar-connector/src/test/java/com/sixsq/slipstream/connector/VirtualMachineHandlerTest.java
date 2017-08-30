package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.persistence.VirtualMachine;
import com.sixsq.slipstream.persistence.Vm;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class VirtualMachineHandlerTest {

    @Test
    public void addVMTest(){


        Vm vm = new Vm("aaa-bb-123", "0123-4567-8912", "Running", "user", true);

        VirtualMachineHandler.addVM(vm);
    }

    @Test
    public void removeVMTest(){


        String instanceID = "24fa782a-0ba0-4930-bcef-697a70b3525d";
        Vm vm = new Vm (instanceID, "0123-4567-8912", "state", "Running", true);

        //VirtualMachineHandler.addVM(vm);
        VirtualMachineHandler.removeVM(vm);

    }

    @Test
    public void updateVMTest(){


        String instanceID = UUID.randomUUID().toString();

        Vm vm = new Vm (instanceID, "0123-4567-8912", "Running", "user", true);


        VirtualMachineHandler.addVM(vm);


        String newState = "Stopped";
        vm.setState(newState);

        VirtualMachineHandler.updateVM(vm);

        VirtualMachine virtualMachine = VirtualMachineHandler.loadVirtualMachine(  instanceID );
        Assert.assertEquals(newState, virtualMachine.getState());

    }
}
