package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.acl.TypePrincipal;
import com.sixsq.slipstream.acl.TypePrincipalRight;
import com.sixsq.slipstream.persistence.VirtualMachine;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.util.SscljProxy;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class VirtualMachineHandlerTest {
    @BeforeClass
    public static void setupClass() {
        SscljProxy.muteForTests();
    }

    @Test
    public void addVMTest() {
        String instanceID = UUID.randomUUID().toString();
        Vm vm = new Vm(instanceID, "0123-4567-8912", "Running", "user", true);

        VirtualMachineHandler.handleVM(vm);
    }

    @Test
    public void removeVMTest() {
        String instanceID = UUID.randomUUID().toString();
        Vm vm = new Vm(instanceID, "0123-4567-8912", "state", "Running", true);

        VirtualMachineHandler.handleVM(vm);
        VirtualMachineHandler.removeVM(vm);
    }

    @Test
    public void updateVMTestChangeState() {
        String instanceID = UUID.randomUUID().toString();
        String cloud = "aCloudName";
        Vm vm = new Vm(instanceID, cloud, "Running", "user", true);
        VirtualMachineHandler.handleVM(vm);

        String newState = "Stopped";
        vm.setState(newState);

        VirtualMachineHandler.handleVM(vm);

        VirtualMachine virtualMachine = VirtualMachineHandler.fetchVirtualMachine(cloud, instanceID);
        if (virtualMachine != null) {
            Assert.assertEquals(newState, virtualMachine.getState());
        }
    }

    @Test
    public void updateVMTestWithRunResource() {
        String instanceID = UUID.randomUUID().toString();
        String cloud = "aCloudName";
        Vm vm = new Vm(instanceID, cloud, "Running", "user", true);
        VirtualMachineHandler.handleVM(vm);

        vm.setRunUuid(UUID.randomUUID().toString());
        String runOwner = "runOwnerName";
        vm.setRunOwner(runOwner);

        VirtualMachineHandler.handleVM(vm);

        VirtualMachine virtualMachine = VirtualMachineHandler.fetchVirtualMachine(cloud, instanceID);
        if (virtualMachine != null) {
            Assert.assertEquals(runOwner, virtualMachine.getDeployment().user);
            List<TypePrincipalRight> rules = virtualMachine.getAcl().getRules();
            Assert.assertNotNull(rules);

            boolean runOwnerFoundInAcl = false;
            for (TypePrincipalRight r : rules) {
                if ((r.getPrincipal() == runOwner) && (r.getType() == TypePrincipal.PrincipalType.USER)) {
                    runOwnerFoundInAcl = true;
                    Assert.assertEquals(TypePrincipalRight.Right.VIEW, r.getRight());
                }
            }

            Assert.assertTrue(runOwnerFoundInAcl);

            Assert.assertEquals(runOwner, virtualMachine.getAcl().getRules());
        }
    }
}
