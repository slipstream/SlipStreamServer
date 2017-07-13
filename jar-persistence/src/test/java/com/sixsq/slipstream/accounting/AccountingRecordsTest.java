package com.sixsq.slipstream.accounting;

import com.sixsq.slipstream.event.ACL;
import com.sixsq.slipstream.event.TypePrincipal;
import com.sixsq.slipstream.event.TypePrincipalRight;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by elegoff on 07.07.17.
 */
public class AccountingRecordsTest {

    @Test
    public void parseEmptyJsonRecords() {
        String jsonAccountingRecords =
                "{\n" +
                        "    \"acl\": {\n" +
                        "        \"owner\": {\n" +
                        "            \"principal\": \"ADMIN\",\n" +
                        "            \"type\": \"ROLE\"\n" +
                        "        },\n" +
                        "        \"rules\": [\n" +
                        "            {\n" +
                        "                \"principal\": \"ADMIN\",\n" +
                        "                \"type\": \"ROLE\",\n" +
                        "                \"right\": \"MODIFY\"\n" +
                        "            },\n" +
                        "            {\n" +
                        "                \"principal\": \"USER\",\n" +
                        "                \"type\": \"ROLE\",\n" +
                        "                \"right\": \"VIEW\"\n" +
                        "            }\n" +
                        "        ]\n" +
                        "    },\n" +
                        "    \"resourceURI\": \"http://sixsq.com/slipstream/1/AccountingRecordCollection\",\n" +
                        "    \"id\": \"accounting-record\",\n" +
                        "    \"operations\": [\n" +
                        "        {\n" +
                        "            \"rel\": \"add\",\n" +
                        "            \"href\": \"accounting-record\"\n" +
                        "        }\n" +
                        "    ],\n" +
                        "    \"accountingRecords\": [],\n" +
                        "    \"count\": 0\n" +
                        "}";
        AccountingRecords ars = AccountingRecords.fromJson(jsonAccountingRecords);
        Assert.assertEquals(0, ars.getAccountingRecords().size());
    }

    @Test
    public void parseJsonAccountingRecords() {

        String jsonRecords = AccountingRecordsTest.jsonAccountingRecords();
        AccountingRecords ars = AccountingRecords.fromJson(jsonRecords);

        Assert.assertEquals(1, ars.getAccountingRecords().size());

        AccountingRecord ar = ars.getAccountingRecords().get(0);

        Assert.assertEquals("mycloud-myvm-42", ar.getIdentifier());

        Assert.assertEquals(1024, ar.getDisk());


    }

    @Test
    public void AccountingRecordCanBeCreatedAndJsonified() {

        TypePrincipal owner = new TypePrincipal(TypePrincipal.PrincipalType.USER, "joe");
        List<TypePrincipalRight> rules = Arrays.asList(new TypePrincipalRight(
                TypePrincipal.PrincipalType.ROLE, "ANON", TypePrincipalRight.Right.ALL));
        ACL acl = new ACL(owner, rules);

        AccountingRecord ar = new AccountingRecord(acl, AccountingRecord.AccountingRecordType.vm, "12313/nodename", new Date(), new Date(), "user", "cloudname",
                Arrays.asList("role1", "role2"), Arrays.asList("group1", "group2"), "realm", "module", "serviceOffer", 1, 64, 1024);

        Assert.assertNotNull(ar.toJson());
    }


    private AccountingRecord createAccoutingRecord(Date startDate, Date stopDate) {
        TypePrincipal owner = new TypePrincipal(TypePrincipal.PrincipalType.USER, "joe");
        List<TypePrincipalRight> rules = Arrays.asList(new TypePrincipalRight(
                TypePrincipal.PrincipalType.ROLE, "ANON", TypePrincipalRight.Right.ALL));
        ACL acl = new ACL(owner, rules);

        AccountingRecord ar = new AccountingRecord(acl, AccountingRecord.AccountingRecordType.vm, "12313/nodename", startDate, stopDate, "user", "cloudname",
                Arrays.asList("role1", "role2"), Arrays.asList("group1", "group2"), "realm", "module", "serviceOffer", 1, 64, 1024);

        return ar;
    }


    @Test
    public void validateAccountingRecord() {
        Date dateInFuture = new Date(Long.MAX_VALUE);
        Date today = new Date();

        Assert.assertTrue(dateInFuture.after(today));


        AccountingRecord validOpen = createAccoutingRecord(today, null);
        AccountingRecord openInFuture = createAccoutingRecord(dateInFuture, null);
        AccountingRecord nullDate = createAccoutingRecord(null, null);
        AccountingRecord bothToday = createAccoutingRecord(today, today);
        AccountingRecord validClosed = createAccoutingRecord(today, dateInFuture);
        AccountingRecord closedButWrongDateOrder = createAccoutingRecord(dateInFuture, today);
        AccountingRecord onlyStop = createAccoutingRecord(null, today);


        Assert.assertTrue(validOpen.isOpenAndValidAccountingRecord());
        Assert.assertFalse(validOpen.isClosedAndValidAccountingRecord());
        Assert.assertTrue(openInFuture.isOpenAndValidAccountingRecord());
        Assert.assertTrue(!openInFuture.isClosedAndValidAccountingRecord());
        Assert.assertFalse(nullDate.isOpenAndValidAccountingRecord());
        Assert.assertFalse(nullDate.isClosedAndValidAccountingRecord());
        Assert.assertFalse(bothToday.isOpenAndValidAccountingRecord());
        Assert.assertFalse(bothToday.isClosedAndValidAccountingRecord());
        Assert.assertFalse(validClosed.isOpenAndValidAccountingRecord());
        Assert.assertTrue(validClosed.isClosedAndValidAccountingRecord());
        Assert.assertFalse(closedButWrongDateOrder.isOpenAndValidAccountingRecord());
        Assert.assertFalse(closedButWrongDateOrder.isClosedAndValidAccountingRecord());
        Assert.assertFalse(onlyStop.isOpenAndValidAccountingRecord());
        Assert.assertFalse(onlyStop.isClosedAndValidAccountingRecord());
    }


    public static String jsonAccountingRecords() {
        return "{\n" +
                "    \"acl\": {\n" +
                "        \"owner\": {\n" +
                "            \"principal\": \"ADMIN\",\n" +
                "            \"type\": \"ROLE\"\n" +
                "        },\n" +
                "        \"rules\": [\n" +
                "            {\n" +
                "                \"principal\": \"ADMIN\",\n" +
                "                \"type\": \"ROLE\",\n" +
                "                \"right\": \"MODIFY\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"principal\": \"USER\",\n" +
                "                \"type\": \"ROLE\",\n" +
                "                \"right\": \"VIEW\"\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"resourceURI\": \"http://sixsq.com/slipstream/1/AccountingRecordCollection\",\n" +
                "    \"id\": \"accounting-record\",\n" +
                "    \"operations\": [\n" +
                "        {\n" +
                "            \"rel\": \"add\",\n" +
                "            \"href\": \"accounting-record\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"accountingRecords\": [\n" +
                "        {\n" +
                "            \"disk\": 1024,\n" +
                "            \"updated\": \"2017-07-07T13:40:57.438Z\",\n" +
                "            \"cloud\": \"FIXME\",\n" +
                "            \"realm\": \"FIXME\",\n" +
                "            \"start\": \"2017-07-07T13:40:51.133Z\",\n" +
                "            \"type\": \"vm\",\n" +
                "            \"created\": \"2017-07-07T13:40:57.438Z\",\n" +
                "            \"ram\": 64,\n" +
                "            \"module\": \"FIXME\",\n" +
                "            \"id\": \"accounting-record/85c68ea2-1569-4112-b881-88a5f0cfe801\",\n" +
                "            \"identifier\": \"mycloud-myvm-42\",\n" +
                "            \"acl\": {\n" +
                "                \"owner\": {\n" +
                "                    \"principal\": \"ADMIN\",\n" +
                "                    \"type\": \"ROLE\"\n" +
                "                },\n" +
                "                \"rules\": [\n" +
                "                    {\n" +
                "                        \"principal\": \"FIXME:view_accounting\",\n" +
                "                        \"type\": \"ROLE\",\n" +
                "                        \"right\": \"VIEW\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"type\": \"ROLE\",\n" +
                "                        \"principal\": \"ADMIN\",\n" +
                "                        \"right\": \"ALL\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"principal\": \"user\",\n" +
                "                        \"type\": \"USER\",\n" +
                "                        \"right\": \"VIEW\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            \"operations\": [\n" +
                "                {\n" +
                "                    \"rel\": \"edit\",\n" +
                "                    \"href\": \"accounting-record/85c68ea2-1569-4112-b881-88a5f0cfe801\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"rel\": \"delete\",\n" +
                "                    \"href\": \"accounting-record/85c68ea2-1569-4112-b881-88a5f0cfe801\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"resourceURI\": \"http://sixsq.com/slipstream/1/AccountingRecord\",\n" +
                "            \"user\": \"user\",\n" +
                "            \"cpu\": 1\n" +
                "        }\n" +
                "    ],\n" +
                "    \"count\": 1\n" +
                "}";


    }

}
