package com.sixsq.slipstream.action.usage;


import java.util.*;


public class MailsBuilder {

    private Date startDate;
    private Date endDate;

    public MailsBuilder(Date startDate, Date endDate){
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Builds emails (MailUsage instances) for each user present in 'nameEmails'
     */
    public List<MailUsage> buildMails(UsageSummaries usageSummaries, Map<String, String> nameEmails) {
        List<MailUsage> result = new ArrayList<MailUsage>();

        Map<String, List<UsageSummary>> usageSummariesPerUser = groupByUser(usageSummaries.usages);

        for(Map.Entry<String, String> nameEmail: nameEmails.entrySet()) {
            String userName = nameEmail.getKey();
            String email = nameEmail.getValue();
            result.add(buildMail(userName, email, usageSummariesPerUser.get(userName)));
        }

        return result;
    }

    private Map<String, List<UsageSummary>> groupByUser(List<UsageSummary> usageSummaries) {

        Map<String, List<UsageSummary>> result = new HashMap<String, List<UsageSummary>>();

        for(UsageSummary usageSummary : usageSummaries){
            List<UsageSummary> summariesForUser = result.get(usageSummary.user);
            if(summariesForUser==null){
                summariesForUser = new ArrayList<UsageSummary>();
                result.put(usageSummary.user, summariesForUser);
            }
            summariesForUser.add(usageSummary);
        }

        return result;
    }

    private MailUsage buildMail(String userName, String email, List<UsageSummary> usages) {
        return new MailUsage(startDate, endDate, userName, email, usages);
    }
}
