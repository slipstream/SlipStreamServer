package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.acl.TypePrincipal;
import com.sixsq.slipstream.acl.TypePrincipalRight;
import com.sixsq.slipstream.persistence.VirtualMachine;
import com.sixsq.slipstream.persistence.Vm;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import com.sixsq.slipstream.ssclj.app.SscljTestServer;

public class VirtualMachineHandlerTest {
    @BeforeClass
    public static void setupClass() {
        SscljTestServer.start();
    }

    @AfterClass
    public static void teardownClass() {
        SscljTestServer.stop();
    }

    @Test
    public void addVMTest() {
        String instanceID = UUID.randomUUID().toString();
        Vm vm = new Vm(instanceID, "cloud", "Running", "user", true);

        VirtualMachineHandler.handleVM(vm);

        VirtualMachine vmResource = VirtualMachineHandler.fetchVirtualMachine("cloud", instanceID);
        Assert.assertNotEquals(null, vmResource);
        Assert.assertEquals(instanceID, vmResource.getInstanceID());
    }

    @Test
    public void removeVMTest() {
        String instanceID = UUID.randomUUID().toString();
        Vm vm = new Vm(instanceID, "cloud", "Running", "user", true);

        VirtualMachineHandler.handleVM(vm);

        VirtualMachine vmResource = VirtualMachineHandler.fetchVirtualMachine("cloud", instanceID);
        Assert.assertNotNull(vmResource);
        Assert.assertEquals(instanceID, vmResource.getInstanceID());

        VirtualMachineHandler.removeVM(vm);
        SscljTestServer.refresh();
        vmResource = VirtualMachineHandler.fetchVirtualMachine("cloud", instanceID);
        Assert.assertNull(vmResource);
    }

    @Test
    public void updateVMChangeStateTest() {
        String instanceID = UUID.randomUUID().toString();
        String cloud = "aCloudName";
        Vm vm = new Vm(instanceID, cloud, "Running", "user", true);
        VirtualMachineHandler.handleVM(vm);
        SscljTestServer.refresh();

        String newState = "Stopped";
        vm.setState(newState);

        VirtualMachineHandler.handleVM(vm);
        SscljTestServer.refresh();

        VirtualMachine virtualMachine = VirtualMachineHandler.fetchVirtualMachine(cloud, instanceID);
        Assert.assertNotNull(virtualMachine);
        Assert.assertEquals(newState, virtualMachine.getState());
    }

    @Test
    public void updateVMWithRunResourceTest() {
        String instanceID = UUID.randomUUID().toString();
        String cloud = "aCloudName";

        Vm vm = new Vm(instanceID, cloud, "Running", "user", true);
        VirtualMachineHandler.handleVM(vm);
        SscljTestServer.refresh();

        vm.setRunUuid(UUID.randomUUID().toString());
        String runOwner = "runOwnerName";
        String runOwnerHref = "user/" + runOwner;
        vm.setRunOwner(runOwner);
        VirtualMachineHandler.handleVM(vm);
        SscljTestServer.refresh();

        VirtualMachine virtualMachine = VirtualMachineHandler.fetchVirtualMachine(cloud, instanceID);
        Assert.assertNotNull(virtualMachine);
        Assert.assertEquals(runOwnerHref, virtualMachine.getDeployment().user.href);
        List<TypePrincipalRight> rules = virtualMachine.getAcl().getRules();
        Assert.assertNotNull(rules);
        TypePrincipalRight right = getUserRuleFromACLRules(runOwner, rules);
        Assert.assertNotEquals(null, right);
        Assert.assertEquals(TypePrincipalRight.Right.VIEW, right.getRight());
    }

    private static TypePrincipalRight getUserRuleFromACLRules(String userName, List<TypePrincipalRight> rules) {
        for (TypePrincipalRight r : rules) {
            if (r.getPrincipal().equals(userName) && r.getType().equals(TypePrincipal.PrincipalType.USER)) {
                return r;
            }
        }
        return null;
    }
}
