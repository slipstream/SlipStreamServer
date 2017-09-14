package com.sixsq.slipstream.persistence;

import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.acl.TypePrincipal;
import com.sixsq.slipstream.acl.TypePrincipalRight;
import com.sixsq.slipstream.util.SscljProxy;
import com.sixsq.slipstream.acl.ACL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Deployment {
    private static final String DEPLOYMENT_RESOURCE = "api/deployment";

    @SuppressWarnings("unused")
    private String id;

    @SuppressWarnings("unused")
    @SerializedName("module-resource-uri")
    private String moduleResourceUri;

    @SuppressWarnings("unused")
    private String type;

    @SuppressWarnings("unused")
    private String category;

    @SuppressWarnings("unused")
    private boolean mutable;

    @SuppressWarnings("unused")
    @SerializedName("keep-running")
    private boolean keepRunning;

    @SuppressWarnings("unused")
    private ArrayList<String> tags;

    @SuppressWarnings("unused")
    private ACL acl;

    @SuppressWarnings("unused")
    private String state;

    @SuppressWarnings("unused")
    private HashMap<String, Node> nodes = new HashMap<>();

    public void setId(String id) {
        this.id = id;
    }

    public void setModuleResourceUri(String moduleResourceUri) {
        this.moduleResourceUri = moduleResourceUri;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

    public void setAcl(ACL acl) {
        this.acl = acl;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }

    public void setNodes(HashMap<String, Node> nodes) {
        this.nodes = nodes;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public String getModuleResourceUri() {
        return moduleResourceUri;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public boolean isMutable() {
        return mutable;
    }

    public ACL getAcl() {
        return acl;
    }

    public String getState() {
        return state;
    }

    public boolean isKeepRunning() {
        return keepRunning;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public HashMap<String, Node> getNodes() {
        return nodes;
    }

    class Node {
        class Parameter {
            String description;
            String value;

            Parameter (String value, String description) {
                this.value = value;
                this.description = description;
            }
        }
        @SerializedName("runtime-parameters")
        HashMap<String, Parameter> runtimeParameters = new HashMap<>();

        Node (String nodeName, Run run) {
            for (RuntimeParameter rp : run.getRuntimeParameters().values()) {
                if (rp.getGroup().equals(nodeName)) {
                    runtimeParameters.put(rp.getName(), new Parameter(rp.getValue(), rp.getDescription()));
                }
            }
        }
    }

    public Deployment(Run run) {
        this.id = "deployment/" + run.getUuid();
        this.moduleResourceUri = run.getRefqname();
        this.type = run.getType().name();
        this.category = run.getCategory().name();
        this.mutable = run.isMutable();
        this.state = run.getState().name();

        for (String nodeName: run.getNodeNamesList()) {
            this.nodes.put(nodeName, new Node(nodeName, run));
        }

        try {
            this.tags = new ArrayList<>(Arrays.asList(run.getRuntimeParameterValue("ss:tags").split(",")));
        } catch (Exception ignored) { }

        TypePrincipal owner = new TypePrincipal(TypePrincipal.PrincipalType.USER, run.getUser());
        List<TypePrincipalRight> rules = new ArrayList<TypePrincipalRight>();
        rules.add(new TypePrincipalRight(TypePrincipal.PrincipalType.USER, run.getUser(), TypePrincipalRight.Right.MODIFY));
        this.acl = new ACL(owner, rules);
    }

    public String toJson() {
        return SscljProxy.toJson(this);
    }

    public static void post(Deployment deployment) {
        SscljProxy.post(DEPLOYMENT_RESOURCE, "internal ADMIN", deployment);
    }
}
