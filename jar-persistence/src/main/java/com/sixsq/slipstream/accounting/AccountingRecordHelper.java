package com.sixsq.slipstream.accounting;

import com.google.gson.JsonObject;
import com.sixsq.slipstream.event.ACL;
import com.sixsq.slipstream.event.TypePrincipal;
import com.sixsq.slipstream.event.TypePrincipalRight;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.util.ModuleUriUtil;
import com.sixsq.slipstream.util.ServiceOffersUtil;
import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Response;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.ROLE;
import static com.sixsq.slipstream.event.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.event.TypePrincipalRight.Right.ALL;

import static com.sixsq.slipstream.util.ServiceOffersUtil.getServiceOfferAttributeAsStringOrNull;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */
public class AccountingRecordHelper {
    private Run run;
    private String nodeInstanceName;
    private JsonObject serviceOffer;



    public AccountingRecordHelper(Run run, String nodeInstanceName) {
        this.run = run;
        this.nodeInstanceName = nodeInstanceName;
        this.serviceOffer = null;
    }

    public Run getRun() {
        return run;
    }

    public String getNodeInstanceName() {
        return nodeInstanceName;
    }

    public static boolean isMuted = false;

    private static final Logger logger = Logger.getLogger(AccountingRecordHelper.class.getName());


    public ACL getACL() {
        String username = run.getUser();

        TypePrincipal owner = new TypePrincipal(USER, username);
        List<TypePrincipalRight> rules = Arrays.asList(
                new TypePrincipalRight(USER, username, ALL),
                new TypePrincipalRight(ROLE, "ADMIN", ALL));
        return new ACL(owner, rules);

    }


    public String getCloudName() {
        String paramName = RuntimeParameter.constructParamName(nodeInstanceName, RuntimeParameter.CLOUD_SERVICE_NAME);
        return run.getRuntimeParameterValueOrDefaultIgnoreAbort(paramName, null);
    }

    public String getUser() {
        return run.getUser();

    }

    public String getModuleName() {
        return ModuleUriUtil.extractModuleNameFromResourceUri(run.getModuleResourceUrl());
    }

    /**
     * @return the service offer id , e.g service-offer/35219a83-ee7f-41ac-b006-291d35504931
     */
    private String getServiceOffer() {
        String paramName = RuntimeParameter.constructParamName(nodeInstanceName, RuntimeParameter.SERVICE_OFFER);
        return run.getRuntimeParameterValueOrDefaultIgnoreAbort(paramName, null);
    }

    private String getInstanceId() {
        String paramName = RuntimeParameter.constructParamName(nodeInstanceName, RuntimeParameter.INSTANCE_ID_KEY);
        return run.getRuntimeParameterValueOrDefaultIgnoreAbort(paramName, null);
    }

    public ServiceOfferRef getServiceOfferRef() {

        String serviceOffer = getServiceOffer();
        if ((serviceOffer != null) && (!serviceOffer.isEmpty())) {
            logger.info(" Service offer found : " + serviceOffer);
            return new ServiceOfferRef(getServiceOffer());
        }

        logger.info("No service offer found");
        //No service offer
        return null;
    }

    /**
     * This method should not be used except by unit tests.
     */
    public void setServiceOffer(JsonObject serviceOffer) {
        this.serviceOffer = serviceOffer;
    }

    public AccountingRecordVM getVmData() {
        String serviceOfferId = getServiceOffer();

        if (serviceOfferId != null && !serviceOfferId.isEmpty() && serviceOffer == null) {
            serviceOffer = ServiceOffersUtil.getServiceOffer(getServiceOffer());
        }

        if (serviceOffer == null) {
            logger.info("VM data can not be inferred form null service offer");
            return new AccountingRecordVM(null, null, null, null);
        }

        Integer cpu = parseInt(getServiceOfferAttributeAsStringOrNull(serviceOffer, ServiceOffersUtil.cpuAttributeName));
        Float ram = parseFloat(getServiceOfferAttributeAsStringOrNull(serviceOffer, ServiceOffersUtil.ramAttributeName));
        Integer disk = parseInt(getServiceOfferAttributeAsStringOrNull(serviceOffer, ServiceOffersUtil.diskAttributeName));
        String instanceType = getServiceOfferAttributeAsStringOrNull(serviceOffer, ServiceOffersUtil.instanceTypeAttributeName);

        return new AccountingRecordVM(cpu, ram, disk, instanceType);
    }

    public String getNodeName(){
        String paramName = RuntimeParameter.constructParamName(nodeInstanceName, RuntimeParameter.NODE_NAME_KEY);
        return run.getRuntimeParameterValueOrDefaultIgnoreAbort(paramName, null);
    }

    public Integer getNodeId(){
        String paramName = RuntimeParameter.constructParamName(nodeInstanceName, RuntimeParameter.NODE_ID_KEY);
        String sNodeId = run.getRuntimeParameterValueOrDefaultIgnoreAbort(paramName, null);
        if (sNodeId == null || sNodeId.isEmpty()) return null;
        return Integer.parseInt(sNodeId);
    }

    public AccountingRecordContext getContext() {
        String instanceId = this.getInstanceId();
        String nodeName = this.getNodeName();
        String runId = this.getRun().getUuid();
        Integer nodeId = this.getNodeId();

        return new AccountingRecordContext(runId, instanceId, nodeName, nodeId);
    }

    private Integer parseInt(String number) {
        return (number == null) ? null : Integer.parseInt(number);
    }

    private Float parseFloat(String number) {
        return (number == null) ? null : Float.parseFloat(number);
    }

    public String getRealm() {
        //FIXME
        return null;
    }

    public List<String> getGroups() {
        //FIXME
        return null;
    }

    public List<String> getRoles() {
        //FIXME
        return null;
    }


    private static AccountingRecord load(AccountingRecordHelper helper) {

        String username = helper.getUser();

        AccountingRecordContext context = helper.getContext();
        String instanceId = context.getInstanceId();
        String nodeName = context.getNodeName();
        String runId = context.getRunId();
        Integer nodeId = context.getNodeId();

        StringBuffer sb = new StringBuffer("context/instanceId='").append(instanceId).append("'");
        if (nodeName != null) {
            sb.append(" and context/nodeName='").append(nodeName).append("'");
        }

        if (nodeId != null) {
            sb.append(" and context/nodeId='").append(nodeId).append("'");
        }

        if (runId != null) {
            sb.append(" and context/runId='").append(runId).append("'");
        }

        String cimiQuery = sb.toString();

        String resource = null;
        try {
            //URLEncoder class performs application/x-www-form-urlencoded-type encoding rather than percent encoding, therefore replacing spaces with +
            resource = AccountingRecord.ACCOUNTING_RECORD_RESOURCE + "?$filter=" + URLEncoder.encode(cimiQuery, "UTF-8").replace("+", "%20");
            Response res = SscljProxy.get(resource, username + " ADMIN");

            if (res == null) return null;

            AccountingRecords records = AccountingRecords.fromJson(res.getEntityAsText());

            if (records == null) return null;

            int nbRecords = records.getAccountingRecords().size();

            switch (nbRecords) {
                case 0: // FIXME :  no corresponding record was started , can't stop it
                    logger.warning("Loading ressource with Query " + resource + " did not return any accounting record" );
                    break;

                case 1: //happy case : a corresponding record was found in ES
                    AccountingRecord ar = records.getAccountingRecords().get(0);
                    logger.info("Found accounting record" + SscljProxy.toJson(ar));
                    if (ar.isOpenAndValidAccountingRecord()) {
                        // The record was properly started, and has not yet been closed
                        logger.info("Accounting record to be closed is valid");
                        return ar;

                    } else {
                        //FIXME : the record we found is invalid
                        logger.warning("Accounting record to be closed is invalid");
                    }
                    break;

                default:
                    //FIXME : more than one record found, need to deal with that
                    logger.warning("Loading ressource with Query " + resource + " did  return too many ("+ nbRecords +") accounting records" );
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void postStartAccountingRecord(Run run, String nodeInstanceName) {

        if (isMuted) {
            return;
        }


        logger.info("Opening accounting record for run :" + run.getUuid() + "on node instance  : " +  nodeInstanceName);
        AccountingRecordHelper helper = new AccountingRecordHelper(run, nodeInstanceName);

        ACL acl = helper.getACL();

        //FIXME : get type from service offer (resource:type) , only VM at the moment
        AccountingRecord.AccountingRecordType type = AccountingRecord.AccountingRecordType.vm;


        String username = helper.getUser();

        String cloud = helper.getCloudName();

        List<String> roles = helper.getRoles();
        List<String> groups = helper.getGroups();
        String realm = helper.getRealm();
        String module = helper.getModuleName();
        ServiceOfferRef serviceOfferRef = helper.getServiceOfferRef();
        AccountingRecordContext context = helper.getContext();
        AccountingRecordVM vmData = helper.getVmData();

        AccountingRecord accountingRecord = new AccountingRecord(acl, type, new Date(), null, username, cloud, roles, groups, realm,
                module, serviceOfferRef, context, vmData.getCpu(), vmData.getRam(), vmData.getDisk(), vmData.getInstanceType());


        //Appending ' ADMIN' to get proper permissions
        String user = username + " ADMIN";
        AccountingRecord.open(accountingRecord, user);

    }


    public static void postStopAccountingRecord(Run run, String nodeInstanceName) {

        if (isMuted) {
            return;
        }


        logger.info("Closing an accounting record for run " + run.getUuid() + "on node instance " +  nodeInstanceName);
        AccountingRecordHelper helper = new AccountingRecordHelper(run, nodeInstanceName);

        AccountingRecord ar = helper.load(helper);



        if (ar != null) {
            ar.setStop(new Date());
            logger.info("Request to SSCLJ for updating Accounting Record " + SscljProxy.toJson(ar));
            AccountingRecord.close(ar, helper.getUser() + " ADMIN");
        } else{
            logger.warning("No AccountingRecord found");
        }

    }

    public static void muteForTests() {
        isMuted = true;
        logger.severe("You should NOT see this message in production: accounting records won't be posted");
    }


}
