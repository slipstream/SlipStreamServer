package com.sixsq.slipstream.persistence;


import com.sixsq.slipstream.acl.ACL;
import com.sixsq.slipstream.acl.TypePrincipal;
import com.sixsq.slipstream.acl.TypePrincipalRight;
import com.sixsq.slipstream.util.SscljProxy;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.sixsq.slipstream.acl.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.acl.TypePrincipalRight.Right.ALL;

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

    public static class UserRef {
        private String href;

        public UserRef(String username){
            this.href = "user/"+username;
        }

        public String getUserName(){
            return this.href.substring(5);
        }
    }


    public static class RunRef{
        private String href;
        private UserRef user;

        public RunRef(String href, UserRef userRef) {
            this.href = href;
            this.user = userRef;
        }

        public String getUserName() {
            return this.user.getUserName();
        }
    };


    @SuppressWarnings("unused")
    private String id;

    public String getId() {
        return id;
    }


    @SuppressWarnings("unused")
    private ACL acl;

    public ACL getAcl() {
        return acl;
    }

    public void setAcl(ACL acl) {
        this.acl = acl;
    }

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

    public RunRef getRun(){
        return this.run;
    }

    public void setCredential(CredentialRef cloudRef) {
        this.credential = cloudRef;
    }
}
