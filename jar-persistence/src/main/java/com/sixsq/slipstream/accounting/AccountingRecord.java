package com.sixsq.slipstream.accounting;

import com.sixsq.slipstream.event.ACL;

import com.sixsq.slipstream.event.TypePrincipal;
import com.sixsq.slipstream.event.TypePrincipalRight;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.util.SscljProxy;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;

import org.restlet.Response;


import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.ROLE;
import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.event.TypePrincipalRight.Right.ALL;


public class AccountingRecord {


    private static final String ACCOUNTING_RECORD_RESOURCE = "api/accounting-record";

    private static final Logger logger = Logger.getLogger(AccountingRecord.class.getName());

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

    /**
     * Build an AccountRecord with a start timestamp
     * from the given run and post it to SSCLJ
     *
     * @param run
     * @return a restlest Response
     */
    public static void postStartAccountingRecord(Run run) {
        ACL acl = AccountingRecord.getACL(run);
        //FIXME : how would the Accounting record type be different from vm ?
        AccountingRecordType type = AccountingRecordType.vm;

        String identifier = AccountingRecord.getIdentifier(run);
        String username = run.getUser();
        String cloud = AccountingRecord.getCloud(run);
        List<String> roles = AccountingRecord.getRoles(run);
        List<String> groups = AccountingRecord.getGroups(run);
        String realm = AccountingRecord.getRealm(run);
        String module = AccountingRecord.getModule(run);
        String serviceOfferRef = AccountingRecord.getServiceOffer(run);
        AccountingRecordVM vmData = AccountingRecord.getVmData(run);

        AccountingRecord accountingRecord = new AccountingRecord(acl, type, identifier, new Date(), null, username, cloud, roles, groups, realm,
                module, serviceOfferRef, vmData.getCpu(), vmData.getRam(), vmData.getDisk());


        //Appending ' ADMIN' to get proper permissions
        String user = username + " ADMIN";
        AccountingRecord.post(accountingRecord, user);

    }

    public static void postStopAccountingRecord(Run run) {


        String username = run.getUser();
        String identifier = AccountingRecord.getIdentifier(run);

        String user = username + " ADMIN";
        AccountingRecord ar = AccountingRecord.getByIdentifier(identifier, user);


        //FIXME

        if (ar == null) return;


        ar.setStop(new Date());
        AccountingRecord.put(ar, user);

    }

    private static ACL getACL(Run run) {
        String username = run.getUser();

        TypePrincipal owner = new TypePrincipal(USER, username);
        List<TypePrincipalRight> rules = Arrays.asList(
                new TypePrincipalRight(USER, username, ALL),
                new TypePrincipalRight(ROLE, "ADMIN", ALL));
        return new ACL(owner, rules);

    }

    private static String getIdentifier(Run run) {
        //FIXME : should be a concatenation like cloudname-vmInstanceId-number
        return "mycloud-myvm-42";

    }

    private static String getCloud(Run run) {
        //FIXME
        return "FIXME";
    }


    private static String getModule(Run run) {
        //FIXME
        return "FIXME";
    }

    private static String getRealm(Run run) {
        //FIXME
        return "FIXME";
    }

    private static String getServiceOffer(Run run) {
        //FIXME
        return null;
    }

    private static List<String> getGroups(Run run) {
        //FIXME
        return null;
    }

    private static List<String> getRoles(Run run) {
        //FIXME
        return null;
    }

    private static AccountingRecordVM getVmData(Run run) {
        //FIXME
        return new AccountingRecordVM(1, 64, 1024);
    }


    public static AccountingRecord getByIdentifier(String identifier, String username) {
        //FIXME
        String cimiQuery = "$filter=accountingRecords/identifier='" + identifier + "'";

        String resource = null;
        try {
            resource = ACCOUNTING_RECORD_RESOURCE + "?" + URLEncoder.encode(cimiQuery, "UTF-8");
            Response res = SscljProxy.get(resource, username);

            AccountingRecords records = AccountingRecords.fromJson(res.getEntityAsText());

            int nbRecords = records.getAccountingRecords().size();

            switch (nbRecords) {
                case 0: // FIXME :  no corresponding record was started , can't stop it
                    break;

                case 1: //happy case : a corresponding record was found in ES
                    AccountingRecord ar = records.getAccountingRecords().get(0);
                    if (ar.isValidStartRecord()) {
                        // The record was properly started, and has not yet been closed
                        return ar;

                    } else {
                        //FIXME : the record we found is invalid
                    }

                    break;


                default: //FIXME : more than one record found, need to deal with that


            }



        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        return null;
    }


    public String toJson() {
        return SscljProxy.toJson(this);
    }

    public static void post(AccountingRecord accountingRecord, String username) {
        SscljProxy.post(ACCOUNTING_RECORD_RESOURCE, username, accountingRecord);
    }

    public static void put(AccountingRecord accountingRecord, String username) {
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
