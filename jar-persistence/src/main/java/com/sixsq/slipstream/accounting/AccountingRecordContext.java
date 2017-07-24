package com.sixsq.slipstream.accounting;

/**
 * Created by elegoff on 17.07.17.
 */
public class AccountingRecordContext {

    private String instanceId;

    private String nodeName;

    private Integer nodeId;

    private String runId;

    public AccountingRecordContext(String runId, String instanceId, String nodeName, Integer nodeId ) {
        this.instanceId = instanceId;
        this.nodeName = nodeName;
        this.runId = runId;
        this.nodeId = nodeId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public String getRunId() {
        return runId;
    }


}
