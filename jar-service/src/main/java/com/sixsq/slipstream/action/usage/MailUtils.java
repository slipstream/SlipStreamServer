package com.sixsq.slipstream.action.usage;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MailUtils {

    private static String encodeQueryParameter(String parameter) {

        int indexEquals = parameter.indexOf('=');
        if(indexEquals<0) {
            return parameter;
        }

        String key = parameter.substring(0, indexEquals);
        String value = parameter.substring(indexEquals+1);

        try {
            return URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
            ignore.printStackTrace();
            return null;
        }
    }

    public static String encodeQueryParameters(String queryString) {
        String[] parameters = queryString.split("&");
        List<String> encodedParameters = new ArrayList<String>();
        for (String parameter : parameters) {
            encodedParameters.add(encodeQueryParameter(parameter));
        }
        return String.join("&", encodedParameters);
    }


    public static String formatDate(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDateFormat.format(date);
    }

    public static String cimiQueryStringUsage(Set<String> userNames, String start, String end) {

        if(userNames == null || userNames.isEmpty()){
            throw new IllegalArgumentException("No user names provided");
        }

        List<String> clauseUsers = new ArrayList<String>();
        for(String userName : userNames) {
            clauseUsers.add(String.format("user='%s'", userName));
        }
        String conditionUsers = String.join(" or ", clauseUsers);

        String queryString = String.format("$filter=start-timestamp=%s and end-timestamp=%s and (%s)",
                start, end, conditionUsers);
        return encodeQueryParameters(queryString);
    }

    public static String cimiQueryStringUsage(String userName, Date start, Date end) {
        Set<String> singleUser = new HashSet<String>(Collections.singletonList(userName));
        return cimiQueryStringUsage(singleUser, MailUtils.formatDate(start), MailUtils.formatDate(end));
    }

    public static String humanFormat(Date date) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
    }

    private static boolean isKBMinuteMetric(String metricName) {
        return metricName != null && "ram".equals(metricName.toLowerCase());
    }

    private static boolean isGBMinuteMetric(String metricName) {
        return metricName != null && "disk".equals(metricName.toLowerCase());
    }

    public static String formatMetricValue(String metricName, double metricValueInMinutes) {

        if (isKBMinuteMetric(metricName)) {
            double metricValueInGBHours = metricValueInMinutes / 60.0 / 1024.0;
            return String.format("%.2f (GBh)", metricValueInGBHours);
        } else if (isGBMinuteMetric(metricName)) {
            double metricValueInGBHours = metricValueInMinutes / 60.0;
            return String.format("%.2f (GBh)", metricValueInGBHours);
        } else {
            double metricValueInHours = metricValueInMinutes / 60.0;
            return String.format("%.2f (h)", metricValueInHours);
        }
    }
}
