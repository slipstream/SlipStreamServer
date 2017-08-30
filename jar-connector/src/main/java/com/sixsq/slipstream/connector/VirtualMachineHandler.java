package com.sixsq.slipstream.connector;


import com.sixsq.slipstream.persistence.VirtualMachine;
import com.sixsq.slipstream.persistence.VirtualMachines;
import com.sixsq.slipstream.persistence.Vm;
import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Response;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class VirtualMachineHandler {

    protected static final String VIRTUAL_MACHINE_RESOURCE = "api/virtual-machine";
    private static final String USERNAME = "internal ADMIN";
    private static final Logger logger = Logger.getLogger(VirtualMachineHandler.class.getName());


    public static void addVM(Vm vm) {
        VirtualMachine vmRecord = VirtualMachineHandler.getResourceFromPojo(vm);
        SscljProxy.post(VIRTUAL_MACHINE_RESOURCE, USERNAME, vmRecord);
    }

    public static void removeVM(Vm vm) {
        VirtualMachine vmDb = loadVirtualMachine(vm.getCloud(), vm.getInstanceId());
        if (vmDb == null) return; //nothing to remove

        VirtualMachine vmRecord = loadVirtualMachine(vm.getCloud(),vm.getInstanceId());
        SscljProxy.delete("api/" + vmDb.getId(), USERNAME, vmRecord);
    }

    public static void updateVM(Vm vm) {
        VirtualMachine vmDb = loadVirtualMachine(vm.getCloud(),vm.getInstanceId());
        if (vmDb == null) return; //nothing to update

        VirtualMachine vmToUpdate = VirtualMachineHandler.getResourceFromPojo(vm);
        SscljProxy.put("api/" + vmDb.getId(), USERNAME, vmToUpdate);

    }

    //Identify a VirtualMachine document in ES from its cloud and instanceID
    public static VirtualMachine loadVirtualMachine( String cloud,String instanceID) {

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

            int nbRecords = records.getVirtualMachines().size();

            switch (nbRecords) {
                case 0: //  no corresponding record was found
                    logger.warning("Loading ressource with Query " + resource + " did not return any  record");
                    break;

                case 1: //happy case : a corresponding record was found in ES
                    virtualMachine = records.getVirtualMachines().get(0);
                    logger.info("Found  record" + SscljProxy.toJson(virtualMachine));
                    break;

                default:
                    // more than one record found, we expect to identify a single document
                    logger.warning("Loading ressource with Query " + resource + " did  return too many (" + nbRecords + ")  records");
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

        return resource;
    }


    private static ACL getAcl() {
        com.sixsq.slipstream.connector.TypePrincipal owner = new com.sixsq.slipstream.connector.TypePrincipal(com.sixsq.slipstream.connector.TypePrincipal.PrincipalType.USER, "joe");
        List<com.sixsq.slipstream.connector.TypePrincipalRight> rules = new ArrayList<com.sixsq.slipstream.connector.TypePrincipalRight>();
        rules.add(new com.sixsq.slipstream.connector.TypePrincipalRight(com.sixsq.slipstream.connector.TypePrincipal.PrincipalType.USER, "joe", com.sixsq.slipstream.connector.TypePrincipalRight.Right.ALL));
        return new ACL(owner, rules);
    }

}
