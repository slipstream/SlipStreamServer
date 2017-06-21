package com.sixsq.slipstream.action.usage;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ValidationException;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * Represents the content of a daily mail sent to one user for a specific day.
 */
public class MailUsage {

    private static final Logger logger = Logger.getLogger(MailUsage.class.getName());

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
        String version = currentVersion();

        String path = "/com/sixsq/slipstream/messages/email_daily_usage_template.html";
        String emailTemplate = readFile(path);

        String userOnInstance = String.format("%s on %s", userName, slipstreamInstanceName());

        return String.format(emailTemplate, userOnInstance, humanDate, usageCloud(), directLink, unsubcribeLink, version);
    }

    private String readFile(String path) {
        InputStream resourceAsStream = getClass().getResourceAsStream(path);
        if(resourceAsStream == null) {
            String message = "Unable to read daily usage email template. Check '" + path + "' is accessible in classpath.";
            throw new RuntimeException(message);
        }
        return new Scanner(resourceAsStream,"UTF-8").useDelimiter("\\A").next();
    }

    private String usageCloud(){

        if (usageSummaries==null || usageSummaries.isEmpty()) {
            return "<div class=\"nothing-to-show\">Your cloud usage is empty.</div>";
        }

        StringBuilder sb = new StringBuilder();

        Collections.sort(usageSummaries, new Comparator<UsageSummary>() {
            @Override
            public int compare(UsageSummary o1, UsageSummary o2) {
                return o1.cloud.compareTo(o2.cloud);
            }
        });

        for(UsageSummary usageSummary : usageSummaries) {
            sb.append(
                "    <div class=\"panel-group\"> <div class=\"panel ss-section panel-default\"> <div class=\"panel-heading ss-section-activator\"> <h4 class=\"panel-title\">\n" +
                usageSummary.cloud  +
                "    </h4> </div><div class=\"panel-collapse collapse in\"> <div class=\"panel-body ss-section-content\"> <div class=\"table-responsive ss-table\"> <table class=\"table table-hover table-condensed\"> <thead>\n" +
                "    <tr><th>Metric</th><th>Quantity</th></tr>\n" +
                "    </thead><tbody>\n");

            for(Map.Entry<String, Double> metric : usageSummary.getMetrics().entrySet()) {
                sb.append(String.format("<tr><td style=\"width:50%%\">%s</td><td style=\"width:50%%\">%s</td></tr>\n", metric.getKey(),
                        MailUtils.formatMetricValue(metric.getKey(), metric.getValue())));
            }
            sb.append("</tbody> </table> </div></div></div></div></div>\n");
        }
        return sb.toString();
    }

    private String unsubcribeLink() {
        return baseUrl() + "/user/" + userName + "?edit=true#general";
    }

    private String linkToDailyUsageJson() {
        String cimiQueryStringUsage = MailUtils.cimiQueryStringUsage(userName, startDate, endDate);
        String resourceURL = String.format("/api/usage-summary?%s", cimiQueryStringUsage);

        return baseUrl() + resourceURL;
    }

    protected String baseUrl() {
        try {
            return Configuration.getInstance().baseUrl;
        } catch(ValidationException ve){
            logger.warning("Unable to get base URL");
            return "SlipStream/";
        }
    }

    private String slipstreamInstanceName() {
        if (baseUrl() == null || baseUrl().isEmpty()) {
            return "";
        }

        int lastIndexOf = baseUrl().lastIndexOf("//");
        return lastIndexOf>0 ? baseUrl().substring(lastIndexOf + 2) : "";
    }

    protected String currentVersion() {
        try {
            return Configuration.getInstance().version;
        } catch (ValidationException ve) {
            logger.warning("Unable to get version:" + ve.getCause());
            return  "";
        }
    }


}
