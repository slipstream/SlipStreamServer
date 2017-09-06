package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.acl.ACL;
import com.sixsq.slipstream.acl.TypePrincipal;
import com.sixsq.slipstream.acl.TypePrincipalRight;
import com.sixsq.slipstream.persistence.VirtualMachine;
import com.sixsq.slipstream.persistence.VirtualMachines;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Response;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;

public class VirtualMachineHandler {
    protected static final String VIRTUAL_MACHINE_RESOURCE = "api/virtual-machine";
    private static final String USERNAME = "internal ADMIN";
    private static final Logger logger = Logger.getLogger(VirtualMachineHandler.class.getName());

    public static void removeVM(Vm vm) {
        VirtualMachine vmDb = fetchVirtualMachine(vm.getCloud(), vm.getInstanceId());
        if (vmDb == null) return; //nothing to remove

        delete(vmDb.getId());
    }

    private static void add(VirtualMachine virtualMachine) {
        SscljProxy.post(VIRTUAL_MACHINE_RESOURCE, USERNAME, virtualMachine);
    }

    private static void update(VirtualMachine virtualMachine, String id) {
        SscljProxy.put("api/" + id, USERNAME, virtualMachine);
    }

    private static void delete(String id) {
        SscljProxy.delete("api/" + id, USERNAME);
    }

    public static void handleVM(Vm vm) {
        VirtualMachine vmRecord = VirtualMachineHandler.getResourceFromPojo(vm);
        VirtualMachine vmDb = fetchVirtualMachine(vm.getCloud(), vm.getInstanceId());

        if (vmDb == null) {
            add(vmRecord);
            return;
        }

        ACL acl = vmDb.getAcl();

        //Update ACL so that any runOwner can view the resource
        //TODO : update code when the credential resource is available
        String runUuid = vm.getRunUuid();

        if (runUuid != null) {
            String runOwner = vm.getRunOwner();
            acl.addRule(new TypePrincipalRight(TypePrincipal.PrincipalType.USER, runOwner, TypePrincipalRight.Right.VIEW));
        }

        //ACL may have been updated
        vmRecord.setAcl(acl);

        update(vmRecord, vmDb.getId());
    }

    //Identify a VirtualMachine document in ES from its cloud and instanceID
    public static VirtualMachine fetchVirtualMachine(String cloud, String instanceID) {

        StringBuffer sb = new StringBuffer("instanceID='").append(instanceID).append("'");
        sb.append(" and credential/href='").append("connector/").append(cloud).append("'");
        String cimiQuery = sb.toString();
        VirtualMachine virtualMachine = null;

        try {
            String resource = VIRTUAL_MACHINE_RESOURCE + "?$filter=" + URLEncoder.encode(cimiQuery, "UTF-8");
            Response res = SscljProxy.get(resource, " internal ADMIN");

            if (res == null) return null;

            VirtualMachines records = VirtualMachines.fromJson(res.getEntityAsText());

            if (records == null) return null;

            List<VirtualMachine> machines = records.getVirtualMachines();
            int nbRecords = machines.size();

            switch (nbRecords) {
                case 0: //  no corresponding record was found
                    logger.warning("Loading ressource with Query " + resource + " did not return any  record");
                    break;

                case 1: //happy case : a corresponding record was found in ES
                    virtualMachine = machines.get(0);
                    logger.info("Found  record" + SscljProxy.toJson(virtualMachine));
                    break;

                default:
                    // more than one record found, we expect to identify a single document

                    logger.warning("Loading ressource with Query " + resource + " did  return too many (" + nbRecords + ")  records");
                    for (VirtualMachine vmToDelete : machines) {
                        delete(vmToDelete.getId());
                    }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return virtualMachine;

    }

    private static VirtualMachine getResourceFromPojo(Vm vm) {
        VirtualMachine resource = new VirtualMachine();

        resource.setInstanceID(vm.getInstanceId());
        resource.setState(vm.getState());
        resource.setIp(vm.getIp());

        String cloudHref = "connector/" + vm.getCloud();
        VirtualMachine.CredentialRef cloudref = new VirtualMachine.CredentialRef(cloudHref);
        resource.setCredential(cloudref);

        //the VM may have a runUuid which will be the run reference of the Virtual Machine CIMI resource
        if (vm.getRunUuid() != null) {

            String runOwner = vm.getRunOwner();
            VirtualMachine.UserRef userRef = new VirtualMachine.UserRef(runOwner);

            String runHref = "run/" + vm.getRunUuid();
            VirtualMachine.RunRef runRef = new VirtualMachine.RunRef(runHref, userRef);
            resource.setRun(runRef);

        }

        return resource;
    }

}
