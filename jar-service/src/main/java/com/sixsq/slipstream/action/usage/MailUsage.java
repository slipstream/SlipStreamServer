package com.sixsq.slipstream.action.usage;

import java.util.List;
import java.util.Map;

/**
 * Represents the content of a daily mail sent to one user for a specific day.
 */
public class MailUsage {

    private String date;
    private String email;
    private String userName;
    private List<UsageSummary> usageSummaries;

    public MailUsage(String date, String userName, String email, List<UsageSummary> usageSummaries){
        this.date = date;
        this.email = email;
        this.userName = userName;
        this.usageSummaries = usageSummaries;
    }

    public String to(){
        return email;
    }

    public String body(){
        return "\n" + intro() + lineSep() + usageCloud() + lineSep() + footer() + lineSep();
    }

    // TODO temp
    private String lineSep() {
        return "--------------------------\n";
    }

    private String intro() {
        return "Hello " + userName + ".\n" +
                "Here is a summary of your cloud usage for the day : " +
                date + ".\n" ;
        // TODO : real text
    }

    private String usageCloud(){

        if(usageSummaries == null || usageSummaries.isEmpty()) {
            return "Your Cloud usage is empty."; // TODO real text
        }

        StringBuilder sb = new StringBuilder();
        for(UsageSummary usageSummary : usageSummaries) {
            sb.append("On Cloud '" + usageSummary.cloud + "'\n");
            // TODO sort by cloud name
            // TODO real text
            for(Map.Entry<String, Double> metric : usageSummary.getMetrics().entrySet()){
                sb.append(metric.getKey()).
                   append(" : ").
                   append(metric.getValue()).
                   append("\n");
            }
        }
        return sb.toString();
    }

    private String footer(){
        // TODO real text
        // TODO real link
        String unsubcribeLink = "http://localhost:8080/user/"+ userName + "#general";
        return
                "You receive this email because you have enabled" +
                "'Receive a mail with cloud usages' in your profile.\n" +
                "To unsubscribe : " + unsubcribeLink;
    }
}
