package com.sixsq.slipstream.accounting;

import com.sixsq.slipstream.event.ACL;
import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.event.TypePrincipal;
import com.sixsq.slipstream.event.TypePrincipalRight;
import com.sixsq.slipstream.util.SscljProxy;

import java.util.*;
import java.util.logging.Logger;

import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.ROLE;
import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.event.TypePrincipalRight.Right.ALL;

public class AccountingRecord {


    private static final String ACCOUNTING_RECORD_RESOURCE = "api/accounting-record";

    private static final Logger logger = Logger.getLogger(Event.class.getName());

    private static final String ACCOUNTING_RECORD_URI = "http://sixsq.com/slipstream/1/AccountingRecord";

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

    @SuppressWarnings("unused")
    private Date start;

    @SuppressWarnings("unused")
    private Date stop;

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

    public void setStop(Date stop) {
        this.stop = stop;
    }



    public AccountingRecord(ACL acl, AccountingRecordType type, String identifier, Date start, String user, String cloud, List<String> roles, List<String> groups,
                            String realm, String module, String serviceOfferRef) {
        this.acl = acl;
        this.resourceURI = ACCOUNTING_RECORD_URI;
        this.created = new Date();
        this.updated = new Date();
        this.type = type;
        this.identifier = identifier;
        this.start = start;
        this.user = user;
        this.cloud = cloud;
        this.roles = roles;
        this.groups = groups;
        this.realm = realm;
        this.module = module;
        this.serviceOfferRef = serviceOfferRef;

    }

    public static enum AccountingRecordType {
        //Virtual machine
        VM,
        //ObjectStore
        OBJ;
    }

    public static void postStartAccountingRecord( String username, AccountingRecordType type, String identifier,  String cloud, List<String> roles, List<String> groups,
                                             String realm, String module, String serviceOfferRef) {
        TypePrincipal owner = new TypePrincipal(USER, username);
        List<TypePrincipalRight> rules = Arrays.asList(
                new TypePrincipalRight(USER, username, ALL),
                new TypePrincipalRight(ROLE, "ADMIN", ALL));
        ACL acl = new ACL(owner, rules);

        AccountingRecord accountingRecord = new AccountingRecord(acl, type, identifier,new Date(), username, cloud, roles, groups, realm, module, serviceOfferRef);

        AccountingRecord.post(accountingRecord);
    }

    public static void postStopAccountingRecord(String username, AccountingRecordType type, String identifier, String cloud, List<String> roles, List<String> groups, String realm, String module, String serviceOfferRef){

        TypePrincipal owner = new TypePrincipal(USER, username);
        List<TypePrincipalRight> rules = Arrays.asList(
                new TypePrincipalRight(USER, username, ALL),
                new TypePrincipalRight(ROLE, "ADMIN", ALL));
        ACL acl = new ACL(owner, rules);

        AccountingRecord accountingRecord = new AccountingRecord(acl, type, identifier,null, username, cloud, roles, groups, realm, module, serviceOfferRef);
        accountingRecord.setStop(new Date());

        AccountingRecord.put(accountingRecord, username);


    }


    public String toJson() {
        return SscljProxy.toJson(this);
    }

    public static void post(AccountingRecord accountingRecord) {
        SscljProxy.post(ACCOUNTING_RECORD_RESOURCE, accountingRecord);
    }

    public static void put(AccountingRecord accountingRecord, String username) {
        SscljProxy.put(ACCOUNTING_RECORD_RESOURCE, username, accountingRecord);
    }


}
