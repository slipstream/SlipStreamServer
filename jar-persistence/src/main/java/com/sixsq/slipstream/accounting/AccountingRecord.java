package com.sixsq.slipstream.accounting;

import com.sixsq.slipstream.event.ACL;


import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.util.SscljProxy;


import java.util.*;
import java.util.logging.Logger;




public class AccountingRecord {



    protected static final String ACCOUNTING_RECORD_RESOURCE = "api/accounting-record";



    protected static final String ACCOUNTING_RECORD_URI = "http://sixsq.com/slipstream/1/AccountingRecord";

    @SuppressWarnings("unused")
    private ACL acl;

    @SuppressWarnings("unused")
    private String resourceURI;

    @SuppressWarnings("unused")
    private Date created;

    @SuppressWarnings("unused")
    private Date updated;

    @SuppressWarnings("unused")
    private AccountingRecordType type;

    @SuppressWarnings("unused")
    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    @SuppressWarnings("unused")
    private Date start;

    public Date getStart() {
        return start;
    }


    @SuppressWarnings("unused")
    private Date stop;

    public Date getStop() {
        return stop;
    }

    @SuppressWarnings("unused")
    private String user;

    @SuppressWarnings("unused")
    private String cloud;

    @SuppressWarnings("unused")
    private List<String> roles;

    @SuppressWarnings("unused")
    private List<String> groups;

    @SuppressWarnings("unused")
    private String realm;

    @SuppressWarnings("unused")
    private String module;

    @SuppressWarnings("unused")
    private String serviceOfferRef;

    //Record Type VM
    @SuppressWarnings("unused")
    long cpu;

    //Record Type VM
    @SuppressWarnings("unused")
    long ram;

    //Record Type VM
    @SuppressWarnings("unused")
    long disk;

    public long getDisk() {
        return disk;
    }

    public void setStop(Date stop) {
        this.stop = stop;
    }


    public AccountingRecord(ACL acl, AccountingRecordType type, String identifier, Date start, Date stop, String user, String cloud, List<String> roles, List<String> groups,
                            String realm, String module, String serviceOfferRef, long cpu, long ram, long disk) {
        this.acl = acl;
        this.resourceURI = ACCOUNTING_RECORD_URI;
        this.created = start;
        this.updated = new Date();
        this.type = type;
        this.identifier = identifier;
        this.start = start;
        this.stop = stop;
        this.user = user;
        this.cloud = cloud;
        this.roles = roles;
        this.groups = groups;
        this.realm = realm;
        this.module = module;
        this.serviceOfferRef = serviceOfferRef;
        this.cpu = cpu;
        this.ram = ram;
        this.disk = disk;

    }

    public static enum AccountingRecordType {
        //Virtual machine
        vm,
        //ObjectStore
        obj;
    }








    public String toJson() {
        return SscljProxy.toJson(this);
    }

    public static void add(AccountingRecord accountingRecord, String username) {
        SscljProxy.post(ACCOUNTING_RECORD_RESOURCE, username, accountingRecord);
    }

    public static void edit(AccountingRecord accountingRecord, String username) {
        SscljProxy.put(ACCOUNTING_RECORD_RESOURCE, username, accountingRecord);
    }

    public boolean isValidStartRecord() {
        boolean isValid = false;

        Date start = this.getStart();
        Date stop = this.getStop();

        //Start date must exist and be in the past
        //Stop date must not be set yet


        return (start != null) && (start.before(new Date()) && (stop == null));
    }




}
