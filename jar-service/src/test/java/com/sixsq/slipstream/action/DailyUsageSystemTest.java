package com.sixsq.slipstream.action;


import org.junit.Test;

public class DailyUsageSystemTest {

    @Test
    public void sendDailyUsages() {
        DailyUsageSender.sendDailyUsageEmails();
    }

}
