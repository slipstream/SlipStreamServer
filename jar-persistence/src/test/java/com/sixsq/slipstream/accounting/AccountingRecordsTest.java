package com.sixsq.slipstream.accounting;

import com.sixsq.slipstream.event.ACL;
import com.sixsq.slipstream.event.TypePrincipal;
import com.sixsq.slipstream.event.TypePrincipalRight;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.fail;

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

        Assert.assertEquals("mynode", ar.getContext().getNodeName());
        Assert.assertEquals(1024, ar.getDisk());
    }

    @Test
    public void AccountingRecordCanBeCreatedAndJsonified() {
        TypePrincipal owner = new TypePrincipal(TypePrincipal.PrincipalType.USER, "joe");
        List<TypePrincipalRight> rules = Arrays.asList(new TypePrincipalRight(
                TypePrincipal.PrincipalType.ROLE, "ANON", TypePrincipalRight.Right.ALL));
        ACL acl = new ACL(owner, rules);

        AccountingRecord ar = createAccoutingRecord(new Date(), new Date());
        Assert.assertNotNull(ar.toJson());
    }


    private AccountingRecord createAccoutingRecord(Date startDate, Date stopDate) {
        TypePrincipal owner = new TypePrincipal(TypePrincipal.PrincipalType.USER, "joe");
        List<TypePrincipalRight> rules = Arrays.asList(new TypePrincipalRight(
                TypePrincipal.PrincipalType.ROLE, "ANON", TypePrincipalRight.Right.ALL));
        ACL acl = new ACL(owner, rules);

        AccountingRecord ar = new AccountingRecord(acl, AccountingRecord.AccountingRecordType.vm,  startDate, stopDate, "user", "cloudname",
                Arrays.asList("role1", "role2"), Arrays.asList("group1", "group2"), "realm", "module",
                new ServiceOfferRef("serviceOffer/638768-768876-878668778"),
                new AccountingRecordContext("c9ae95f1-caee-4b5e-a3f9-727d11355146", "60117142-7a77-4e72-9de3-93ee5f547006", "my-node-name", 42), 1, 64, 1024, "instanceType");
        ar.setId("accounting-record/6dd4dbc8-a85a-4abc-82b4-7b032b8cc07b");
        return ar;
    }

    @Test
    public void validJsonServiceOfferRef() {

        Date today = new Date();
        SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        jsonDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String sToday = jsonDateFormat.format(today);
        AccountingRecord ar = createAccoutingRecord(today, null);

        //trick to get a fixed timestamp
        ar.setUpdated(today);

        String json = ar.toJson();

        String jsonWithoutServiceOffer = "{\n" +
                "  \"acl\": {\n" +
                "    \"owner\": {\n" +
                "      \"type\": \"USER\",\n" +
                "      \"principal\": \"joe\"\n" +
                "    },\n" +
                "    \"rules\": [\n" +
                "      {\n" +
                "        \"right\": \"ALL\",\n" +
                "        \"type\": \"ROLE\",\n" +
                "        \"principal\": \"ANON\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"resourceURI\": \"http://sixsq.com/slipstream/1/AccountingRecord\",\n" +
                "  \"created\": \"" + sToday + "\",\n" +
                "  \"updated\": \"" + sToday + "\",\n" +
                "  \"type\": \"vm\",\n" +
                "  \"start\": \"" + sToday + "\",\n" +
                "  \"user\": \"user\",\n" +
                "  \"cloud\": \"cloudname\",\n" +
                "  \"roles\": [\n" +
                "    \"role1\",\n" +
                "    \"role2\"\n" +
                "  ],\n" +
                "  \"groups\": [\n" +
                "    \"group1\",\n" +
                "    \"group2\"\n" +
                "  ],\n" +
                "  \"realm\": \"realm\",\n" +
                "  \"module\": \"module\",\n" +
                /* This JSON is explicitly wrong be it doesn't contain serviceOffer info */

                //"  \"serviceOffer\": {\n" +
                //"    \"href\": \"serviceOffer/638768-768876-878668778\"\n" +
                //"  },\n" +
                "  \"cpu\": 1,\n" +
                "  \"ram\": 64.0,\n" +
                "  \"disk\": 1024,\n" +
                "  \"instanceType\": \"instanceType\"\n" +
                "}";

        String expectedJSONString = "{\n" +
                "  \"acl\": {\n" +
                "    \"owner\": {\n" +
                "      \"type\": \"USER\",\n" +
                "      \"principal\": \"joe\"\n" +
                "    },\n" +
                "    \"rules\": [\n" +
                "      {\n" +
                "        \"right\": \"ALL\",\n" +
                "        \"type\": \"ROLE\",\n" +
                "        \"principal\": \"ANON\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"id\": \"accounting-record/6dd4dbc8-a85a-4abc-82b4-7b032b8cc07b\",\n" +
                "  \"resourceURI\": \"http://sixsq.com/slipstream/1/AccountingRecord\",\n" +
                "  \"created\": \"" + sToday + "\",\n" +
                "  \"updated\": \"" + sToday + "\",\n" +
                "  \"type\": \"vm\",\n" +
                "  \"start\": \"" + sToday + "\",\n" +
                "  \"user\": \"user\",\n" +
                "  \"cloud\": \"cloudname\",\n" +
                "  \"roles\": [\n" +
                "    \"role1\",\n" +
                "    \"role2\"\n" +
                "  ],\n" +
                "  \"groups\": [\n" +
                "    \"group1\",\n" +
                "    \"group2\"\n" +
                "  ],\n" +
                "  \"realm\": \"realm\",\n" +
                "  \"module\": \"module\",\n" +
                "  \"serviceOffer\": {\n" +
                "    \"href\": \"serviceOffer/638768-768876-878668778\"\n" +
                "  },\n" +
                "  \"context\": {\n" +
                "    \"instanceId\": \"60117142-7a77-4e72-9de3-93ee5f547006\",\n" +
                "    \"nodeName\": \"my-node-name\",\n" +
                "    \"nodeId\": 42,\n" +
                "    \"runId\": \"c9ae95f1-caee-4b5e-a3f9-727d11355146\"\n" +
                "  },\n" +
                "  \"cpu\": 1,\n" +
                "  \"ram\": 64.0,\n" +
                "  \"disk\": 1024,\n" +
                "  \"instanceType\": \"instanceType\"\n" +
                "}";


        try {
            JSONAssert.assertEquals(expectedJSONString, json, true);
            JSONAssert.assertNotEquals(jsonWithoutServiceOffer, json, true);

        } catch (JSONException e) {
            fail();
        }
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
                "            \"cpu\": 1,\n" +
                "            \"serviceOffer\": {\"href\" : \"service-offer/35219a83-ee7f-41ac-b006-291d35504931\" },\n" +
                "             \"context\": {\n" +
                "                               \"instanceId\": \"c9ae95f1-caee-4b5e-a3f9-727d11355146\",\n" +
                "                               \"nodeName\": \"mynode\",\n" +
                "                               \"runId\": \"60117142-7a77-4e72-9de3-93ee5f547006\"\n" +
                "                          }\n" +
                "        }\n" +
                "    ],\n" +
                "    \"count\": 1\n" +
                "}";


    }

}
