package com.sixsq.slipstream.persistence;

import java.util.Calendar;
import java.util.Date;

import java.util.List;

public class DeleteRuns {
    public static void main( String[] args ) {
        System.out.println("Start.");
        int threeMonthMin = 3 * 31 * 24 * 60;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -threeMonthMin);
        Date back = calendar.getTime();
        System.out.println("Date back: " + back);
        List<Run> oldRuns = Run.listAllFinishedBefore(threeMonthMin);
        System.out.println("-- Number of old runs: " + oldRuns.size());
        int i = 1;
        for (Run r: oldRuns) {
            System.out.println(String.format("%5d run: %s, start: %s", i, r.getUuid(), r.getStart()));
            r.remove();
            i++;
        }
        System.out.println("Done");
        System.exit(0);
    }
}
