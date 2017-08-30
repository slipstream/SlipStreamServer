package com.sixsq.slipstream.persistence;


import com.sixsq.slipstream.event.ACL;
import com.sixsq.slipstream.event.TypePrincipal;
import com.sixsq.slipstream.event.TypePrincipalRight;
import com.sixsq.slipstream.util.SscljProxy;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.event.TypePrincipalRight.Right.ALL;

public class VirtualMachine {

    public String toJson() {
        return SscljProxy.toJson(this);
    }
    private class ServiceOfferRef{
        private String href;

        public ServiceOfferRef(String href) {
            this.href = href;
        }

    };

    public static class CredentialRef {

        private String href;

        public CredentialRef(String href) {
            this.href = href;
        }
    };

    private class RunRef{

        private String href;

        public RunRef(String href) {
            this.href = href;
        }
    };


    @SuppressWarnings("unused")
    private String id;

    public String getId() {
        return id;
    }


    @SuppressWarnings("unused")
    private ACL acl;

    @SuppressWarnings("unused")
    private String resourceURI;

    @SuppressWarnings("unused")
    private Date created;

    @SuppressWarnings("unused")
    private Date updated;


    @SuppressWarnings("unused")
    private String instanceID;

    @SuppressWarnings("unused")
    private String state;

    public String getState() {
        return state;
    }

    @SuppressWarnings("unused")
    private String ip;

    @SuppressWarnings("unused")
    private ServiceOfferRef serviceOffer;
    @SuppressWarnings("unused")
    private RunRef run;
    @SuppressWarnings("unused")
    private CredentialRef credential;

    public VirtualMachine() {
        TypePrincipal owner = new TypePrincipal(USER, "ADMIN");
        List<TypePrincipalRight> rules = Arrays.asList(new TypePrincipalRight(USER, "ADMIN", ALL));
        this.acl= new ACL(owner, rules);

        this.resourceURI ="http://sixsq.com/slipstream/1/VirtualMachine";
        this.created = new Date();
        this.updated = new Date();
    }





    public void setInstanceID(String instanceID) {
        this.instanceID = instanceID;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setServiceOffer(ServiceOfferRef serviceOfferRef) {
        this.serviceOffer = serviceOfferRef;
    }

    public void setRun(RunRef runRef) {
        this.run = runRef;
    }

    public void setCredential(CredentialRef cloudRef) {
        this.credential = cloudRef;
    }
}
