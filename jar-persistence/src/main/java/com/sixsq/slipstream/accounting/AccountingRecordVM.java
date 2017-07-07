package com.sixsq.slipstream.accounting;

/**
 * Created by elegoff on 07.07.17.
 */
public class AccountingRecordVM {

    private long cpu, ram, disk;

    public AccountingRecordVM(long cpu, long ram, long disk) {
        this.cpu = cpu;
        this.ram = ram;
        this.disk = disk;
    }

    public long getCpu() {
        return cpu;
    }

    public long getRam() {
        return ram;
    }

    public long getDisk() {
        return disk;
    }
}
