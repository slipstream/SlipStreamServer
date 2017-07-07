package com.sixsq.slipstream.accounting;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
/**
 * Created by elegoff on 07.07.17.
 */
public class AccountingRecords {




    private List<AccountingRecord> accountingRecords = new ArrayList<AccountingRecord>();

    public List<AccountingRecord> getAccountingRecords() {
        return accountingRecords;
    }

    public static AccountingRecords fromJson(String jsonRecords){
        Gson gson = new Gson();
        return gson.fromJson(jsonRecords, AccountingRecords.class);
    }


    }
