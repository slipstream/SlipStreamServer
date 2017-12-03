package com.sixsq.slipstream.connector.dummy;

import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.credentials.CloudCredential;
import com.sixsq.slipstream.credentials.HRef;
import com.sixsq.slipstream.credentials.ICloudCredential;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.UserParameter;

import java.util.Map;

public class DummyCloudCredDef extends CloudCredential<DummyCloudCredDef> {

    public String href = "credential-template/store-cloud-cred-dummy";

    @SerializedName("domain-name")
    public String domainName;

    public DummyCloudCredDef(String instanceName, String key, String secret,
                             String domainName) {
        super(new HRef("connector/" + instanceName), key, secret);
        this.domainName = domainName;
    }

    public DummyCloudCredDef(String instanceName, String key, String secret,
                             String domainName, Integer quota) {

        this(instanceName, key, secret, domainName);
        this.quota = quota;
    }

    public DummyCloudCredDef(String instanceName, Map<String, UserParameter> params) {
        super(new HRef("connector/" + instanceName));
        setParams(params);
    }

    public void setParams(Map<String, UserParameter> params) {
        super.setParams(params);

        String instanceName = getConnectorInstanceName();
        String k;

        k = UserParameter.constructKey(instanceName, "domain", "name");
        if (params.containsKey(k)) {
            domainName = params.get(k).getValue();
        }
    }

    public Map<String, UserParameter> getParams() throws ValidationException {
        Map<String, UserParameter> params = super.getParams();

        String instanceName = getConnectorInstanceName();
        String k;

        if (null != this.domainName) {
            k = UserParameter.constructKey(instanceName, "domain", "name");
            params.put(k, new UserParameter(k, this.domainName, ""));
        }
        return params;
    }

    @Override
    public DummyCloudCredDef fromJson(String json) {
        return (DummyCloudCredDef) fromJson(json, DummyCloudCredDef.class);
    }

    public boolean credEquals(ICloudCredential<DummyCloudCredDef> other) {
        DummyCloudCredDef o = (DummyCloudCredDef) other;
        return this.key.equals(o.key) && this.secret.equals(o.secret)
                && this.domainName.equals(o.domainName);
    }
}


