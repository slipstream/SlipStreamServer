package com.sixsq.slipstream.accounting;

import com.sixsq.slipstream.event.ACL;
import com.sixsq.slipstream.event.TypePrincipal;
import com.sixsq.slipstream.event.TypePrincipalRight;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Response;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.ROLE;
import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.event.TypePrincipalRight.Right.ALL;

/**
 * Created by elegoff on 10.07.17.
 */
public class AccountingRecordHelper {
    private static Run run;

    public static boolean isMuted = false;

    private static final Logger logger = Logger.getLogger(AccountingRecordHelper.class.getName());


    private static ACL getACL() {
        String username = run.getUser();

        TypePrincipal owner = new TypePrincipal(USER, username);
        List<TypePrincipalRight> rules = Arrays.asList(
                new TypePrincipalRight(USER, username, ALL),
                new TypePrincipalRight(ROLE, "ADMIN", ALL));
        return new ACL(owner, rules);

    }

    private static String getIdentifier() {
        //FIXME : should be a concatenation like cloudname-vmInstanceId-number
        return "mycloud-myvm-20170710";

    }


    private static String getNodeName() {
        List<String> nodeNames = run.getNodeNamesList();


        //FIXME : take first ? check if empty
        String nodeName = nodeNames.get(0);

        return nodeName;

    }


    protected static String getCloudName() throws NotFoundException, AbortException {
        //FIXME

        return "mycloudname";

    }


    private static String getModule() {
        //FIXME
        return "my-module";
    }

    private static String getRealm() {
        //FIXME
        return "my-realm";
    }

    private static String getServiceOffer() {
        //FIXME
        return null;
    }

    private static List<String> getGroups() {
        //FIXME
        return null;
    }

    private static List<String> getRoles() {
        //FIXME
        return null;
    }

    private static AccountingRecordVM getVmData() {
        //FIXME
        return new AccountingRecordVM(1, 64, 1024);
    }


    private static AccountingRecord getByIdentifier(String identifier, String username) {
        //FIXME
        String cimiQuery = "$filter=accountingRecords/identifier='" + identifier + "'";

        String resource = null;
        try {
            resource = AccountingRecord.ACCOUNTING_RECORD_RESOURCE + "?" + URLEncoder.encode(cimiQuery, "UTF-8");
            Response res = SscljProxy.get(resource, username);

            AccountingRecords records = AccountingRecords.fromJson(res.getEntityAsText());

            if (records == null) return null;

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

    public static void postStartAccountingRecord(Run run) {

        if (isMuted) {
            return;
        }

        AccountingRecordHelper.run = run;

        ACL acl = AccountingRecordHelper.getACL();

        //FIXME : how would the Accounting record type be different from vm ?
        AccountingRecord.AccountingRecordType type = AccountingRecord.AccountingRecordType.vm;

        String identifier = AccountingRecordHelper.getIdentifier();
        String username = AccountingRecordHelper.run.getUser();
        String cloud = "";
        try {
            cloud = AccountingRecordHelper.getCloudName();
        } catch (NotFoundException e) {
            logger.log(Level.WARNING, e.getMessage());
            cloud = "NOTFOUND";
        } catch (AbortException e) {
            cloud = "ERROR";
            logger.log(Level.WARNING, e.getMessage());
        }
        List<String> roles = AccountingRecordHelper.getRoles();
        List<String> groups = AccountingRecordHelper.getGroups();
        String realm = AccountingRecordHelper.getRealm();
        String module = AccountingRecordHelper.getModule();
        String serviceOfferRef = AccountingRecordHelper.getServiceOffer();
        AccountingRecordVM vmData = AccountingRecordHelper.getVmData();

        AccountingRecord accountingRecord = new AccountingRecord(acl, type, identifier, new Date(), null, username, cloud, roles, groups, realm,
                module, serviceOfferRef, vmData.getCpu(), vmData.getRam(), vmData.getDisk());


        //Appending ' ADMIN' to get proper permissions
        String user = username + " ADMIN";
        AccountingRecord.add(accountingRecord, user);

    }

    public static void postStopAccountingRecord(Run run) {

        if (isMuted) {
            return;
        }

        AccountingRecordHelper.run = run;
        String username = AccountingRecordHelper.run.getUser();
        String identifier = AccountingRecordHelper.getIdentifier();

        String user = username + " ADMIN";
        AccountingRecord ar = AccountingRecordHelper.getByIdentifier(identifier, user);


        //FIXME

        if (ar == null) return;


        ar.setStop(new Date());
        AccountingRecord.edit(ar, user);

    }

    public static void muteForTests() {
        isMuted = true;
        logger.severe("You should NOT see this message in production: events won't be posted");
    }

    public static void unMuteForSomeTests() {
        isMuted = false;
        logger.info("Accounting Records will be posted");
    }
}
