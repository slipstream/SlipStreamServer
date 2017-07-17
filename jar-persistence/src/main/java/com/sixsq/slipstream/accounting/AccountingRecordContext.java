package com.sixsq.slipstream.accounting;

/**
 * Created by elegoff on 17.07.17.
 */
public class AccountingRecordContext {

    private String instanceId;

    private String nodeName;

    private String runId;

    public AccountingRecordContext(String instanceId, String nodeName, String runId) {
        this.instanceId = instanceId;
        this.nodeName = nodeName;
        this.runId = runId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getRunId() {
        return runId;
    }


}
