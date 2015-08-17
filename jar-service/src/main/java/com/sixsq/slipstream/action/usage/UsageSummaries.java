package com.sixsq.slipstream.action.usage;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class UsageSummaries {

    public List<UsageSummary> usages = new ArrayList<UsageSummary>();

    public static UsageSummaries fromJson(String jsonUsages){
        Gson gson = new Gson();
        return gson.fromJson(jsonUsages, UsageSummaries.class);
    }

}
