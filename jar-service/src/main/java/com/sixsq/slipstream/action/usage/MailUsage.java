package com.sixsq.slipstream.action.usage;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.messages.MessageUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents the content of a daily mail sent to one user for a specific day.
 */
public class MailUsage {

    private static final Logger logger = Logger.getLogger(MailUsage.class.getName());

    private static final String MSG_DAILY_USAGE = "MSG_DAILY_USAGE";

    private Date startDate;
    private Date endDate;
    private String email;
    private String userName;
    private List<UsageSummary> usageSummaries;

    public MailUsage(Date startDate, Date endDate, String userName, String email, List<UsageSummary> usageSummaries){
        this.startDate = startDate;
        this.endDate = endDate;
        this.email = email;
        this.userName = userName;
        this.usageSummaries = usageSummaries;
    }

    public String to(){
        return email;
    }

    public String body(){
        String unsubcribeLink = unsubcribeLink();
        String directLink = linkToDailyUsageJson();
        String humanDate = MailUtils.humanFormat(startDate);
        return MessageUtils.format(MSG_DAILY_USAGE, userName, humanDate, usageCloud(), unsubcribeLink, directLink);
    }

    private String usageCloud(){

        if(usageSummaries == null || usageSummaries.isEmpty()) {
            return "You have no cloud usage recorded for this period.";
        }

        StringBuilder sb = new StringBuilder();
        for(UsageSummary usageSummary : usageSummaries) {
            sb.append("Cloud <b>" + usageSummary.cloud + "</b> <p/>");

            sb.append("<table border=\"1\">");
            sb.append("<tr><th>Metric</th><th>Quantity (Unit * minutes)</th></tr>");

            // TODO sort by cloud name
            for(Map.Entry<String, Double> metric : usageSummary.getMetrics().entrySet()){
                sb.append(String.format("<tr><td>%s</td><td>%s</th></tr>", metric.getKey(), metric.getValue()));
            }
            sb.append("</table><p />");
        }
        return sb.toString();
    }

    private String unsubcribeLink() {
        try {
            return Configuration.getInstance().baseUrl + "/user/" + userName + "#general";
        }catch(ValidationException ve){
            logger.warning("Unable to get base URL");
            return null;
        }
    }

    private String linkToDailyUsageJson() {
        try {
            String cimiQueryStringUsage = MailUtils.cimiQueryStringUsage(userName, startDate, endDate);
            String resourceURL = String.format("/usage?%s", cimiQueryStringUsage);

            return Configuration.getInstance().baseUrl + resourceURL;
        }catch(ValidationException ve){
            logger.warning("Unable to get base URL");
            return null;
        }
    }
}
