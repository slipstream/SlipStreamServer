package com.sixsq.slipstream.credentials;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.connector.Connector;
import com.sixsq.slipstream.connector.CloudConnector;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.QuotaParameter;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Response;
import org.restlet.data.Form;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudCredential<T> implements ICloudCredential<T> {
    public HRef connector;
    public String id;

    public String key;

    public String secret;

    public Integer quota = 20;

    private transient static final Logger logger = Logger.getLogger(
            "com.sixsq.slipstream.credentials." + CloudCredential.class.getName());

    public CloudCredential(HRef connector, String key, String secret) {
        this.connector = connector;
        this.key = key;
        this.secret = secret;
    }

    public CloudCredential(HRef connector) {
        this.connector = connector;
    }

    public String getConnectorInstanceName() {
        return this.connector.getRefResourceName();
    }

    public String toJson() {
        return (new Gson()).toJson(this);
    }

    public static Object fromJson(String json, Class klass) {
        return (new Gson()).fromJson(json, klass);
    }

    public boolean equalsTo(ICloudCredential<T> other) {
        return getConnectorInstanceName().equals((other)
                .getConnectorInstanceName()) && credEquals(other);
    }

    /**
     * This will persist only class attributes.  No credential parameters
     * from User will be persisted.
     * <p>
     * search (order by created, take last) / merge / create new / delete old
     */
    public void store(User user) throws ValidationException {
        // search for last modified
        String filter = "type^='cloud-cred' and connector/href='" + connector.href + "'";
        Form queryParameters = new Form();
        queryParameters.add("$filter", filter);
        queryParameters.add("$orderby", "updated:desc");
        queryParameters.add("$last", "1");

        String authz = user.getName() + " USER";

        Response resp = SscljProxy.get(SscljProxy.CREDENTIAL_RESOURCE, authz, queryParameters);
        CloudCredentialCollection cc = (CloudCredentialCollection) fromJson(resp.getEntityAsText(), CloudCredentialCollection.class);
        List<CloudCredential> credList = cc.getCredentials();
        if (credList.isEmpty()){
            // Create only if the base part of the credentials is properly defined.
            if (cloudCredsDefined()) {
                SscljProxy.post(SscljProxy.CREDENTIAL_RESOURCE, authz, new CloudCredentialCreateTmpl(this), true);
            } else {
                logger.warning("Base part of credentials for connector instance '" +
                        connector.href + "' is not properly defined for user '" +
                        user.getName() + "'. Not creating.");
            }
        } else{
            // Edit

            // merge
            CloudCredential credOld = (CloudCredential) cc.getCredentials().get(0);
            merge(credOld);

            // create new
            SscljProxy.post(SscljProxy.CREDENTIAL_RESOURCE, authz, new CloudCredentialCreateTmpl(this), true);

            // delete old
            SscljProxy.delete(SscljProxy.BASE_RESOURCE + credOld.id, authz, true);
        }
    }

    public void merge(ICloudCredential<T> other) throws ValidationException {
        Map<String, UserParameter> otherParams = other.getParams();
        otherParams.putAll(getParams());
        setParams(otherParams);
    }

    @Override
    public ICloudCredential fromJson(String json) {
        return null;
    }

    @Override
    public boolean credEquals(ICloudCredential<T> other) {
        return false;
    }

    @Override
    public void setParams(Map<String, UserParameter> params) {
        String instanceName = getConnectorInstanceName();
        String k;

        k = UserParameter.constructKey(instanceName,
                UserParametersFactoryBase.KEY_PARAMETER_NAME);
        if (params.containsKey(k)) {
            key = params.get(k).getValue();
        }
        k = UserParameter.constructKey(instanceName,
                UserParametersFactoryBase.SECRET_PARAMETER_NAME);
        if (params.containsKey(k)) {
            secret = params.get(k).getValue();
        }
        k = UserParameter.constructKey(instanceName, QuotaParameter.QUOTA_VM_PARAMETER_NAME);
        if (params.containsKey(k)) {
            String value = params.get(k).getValue();
            if (!value.isEmpty()) {
                quota = Integer.parseInt(value);
            }
        }
    }

    private boolean isKeySet() {
        return null != this.key && !this.key.isEmpty();
    }

    private boolean isSecretSet() {
        return null != this.secret && !this.secret.isEmpty();
    }

    public boolean cloudCredsDefined() {
        return isKeySet() && isSecretSet();
    }

    @Override
    public Map<String, UserParameter> getParams() throws ValidationException {
        Map<String, UserParameter> params = new HashMap<>();

        String instanceName = getConnectorInstanceName();
        String k;

        if (null != this.key) {
            k = UserParameter.constructKey(instanceName,
                    UserParametersFactoryBase.KEY_PARAMETER_NAME);
            params.put(k, new UserParameter(k, this.key, ""));
        }
        if (null != this.secret) {
            k = UserParameter.constructKey(instanceName,
                    UserParametersFactoryBase.SECRET_PARAMETER_NAME);
            params.put(k, new UserParameter(k, this.secret, ""));
        }
        if (null != this.quota) {
            k = UserParameter.constructKey(instanceName, QuotaParameter.QUOTA_VM_PARAMETER_NAME);
            params.put(k, new UserParameter(k, String.valueOf(this.quota), ""));
        }
        return params;
    }
}
