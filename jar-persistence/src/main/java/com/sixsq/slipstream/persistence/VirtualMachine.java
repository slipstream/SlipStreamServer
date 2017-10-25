package com.sixsq.slipstream.persistence;


import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.acl.ACL;
import com.sixsq.slipstream.acl.TypePrincipal;
import com.sixsq.slipstream.acl.TypePrincipalRight;
import com.sixsq.slipstream.util.SscljProxy;

import java.util.*;

import static com.sixsq.slipstream.acl.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.acl.TypePrincipalRight.Right.ALL;

public class VirtualMachine {

    public String toJson() {
        return SscljProxy.toJson(this);
    }

    public static class ServiceOfferRef{
        public final String href;

        @SerializedName("resource:vcpu")
        public final Integer resourceVcpu;

        @SerializedName("resource:ram")
        public final Float resourceRam;

        @SerializedName("resource:disk")
        public final Float resourceDisk;

        @SerializedName("resource:instanceType")
        public final String resourceInstanceType;

        @SerializedName("price:currency")
        public final String priceCurrency;

        @SerializedName("price:unitCost")
        public final Float priceUnitCost;

        @SerializedName("price:billingPeriodCode")
        public final String priceBillingPeriodCode;

        @SerializedName("price:freeUnits")
        public final Float priceFreeUnits;

        @SerializedName("price:unitCode")
        public final String priceUnitCode;

        public ServiceOfferRef(String href) {
            this(href, null, null, null, null, null, null, null, null, null);
        }

        public ServiceOfferRef(String href, Integer resourceVcpu, Float resourceRam, Float resourceDisk,
                               String resourceInstanceType, String priceCurrency, Float priceUnitCost,
                               String priceBillingPeriodCode, Float priceFreeUnits, String priceUnitCode) {
            this.href = href;

            this.resourceVcpu = resourceVcpu;
            this.resourceRam = resourceRam;
            this.resourceDisk = resourceDisk;
            this.resourceInstanceType = resourceInstanceType;

            this.priceCurrency = priceCurrency;
            this.priceUnitCost = priceUnitCost;
            this.priceBillingPeriodCode = priceBillingPeriodCode;
            this.priceFreeUnits = priceFreeUnits;
            this.priceUnitCode = priceUnitCode;
        }
    };

    public static class CredentialRef {
        public final String href;

        public CredentialRef(String href) {
            this.href = href;
        }

        public boolean equals(CredentialRef credential) {
            return this.href.equals(credential.href);
        }
    };

    public static class UserRef {
        public final String href;
        public final String organization;

        public UserRef(String username){
            this(username, null);
        }

        public UserRef(String username, String organization){
            this.href = "user/" + username;
            this.organization = organization;
        }

    }

    public static class CloudRef {
        public final String href;

        public CloudRef(String cloud){
            this.href = "connector/" + cloud;
        }

    }

    public static class DeploymentRef{
        public final String href;
        public final UserRef user;

        public DeploymentRef(String href, UserRef userRef) {
            this.href = href;
            this.user = userRef;
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

    private CloudRef connector;

    public void setConnector(CloudRef connector) {
        this.connector = connector;
    }

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
    private DeploymentRef deployment;

    @SuppressWarnings("unused")
    private Set<CredentialRef> credentials;

    public VirtualMachine() {
        TypePrincipal owner = new TypePrincipal(USER, "ADMIN");
        List<TypePrincipalRight> rules = Arrays.asList(new TypePrincipalRight(USER, "ADMIN", ALL));
        this.acl= new ACL(owner, rules);
        this.credentials = Collections.synchronizedSet(new HashSet<>());
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

    public void setDeployment(DeploymentRef deploymentRef) {
        this.deployment = deploymentRef;
    }

    public DeploymentRef getDeployment(){
        return this.deployment;
    }

    public Set<CredentialRef> getCredentials() {
        return this.credentials;
    }

    public void addCredential(CredentialRef cloudRef) {
        for(CredentialRef c : this.credentials){
            if (c.equals(cloudRef)) {
                return;
            }
        }
        this.credentials.add(cloudRef);
    }
}
