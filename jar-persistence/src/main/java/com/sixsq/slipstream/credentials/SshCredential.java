package com.sixsq.slipstream.credentials;

import com.google.gson.Gson;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.UserParameter;
import com.sixsq.slipstream.util.SscljProxy;
import org.restlet.Response;
import org.restlet.data.Form;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SshCredential<T> implements ISshCredential<T> {
    public String href = "credential-template/import-ssh-public-key";

    public String id;

    public String publicKey;

    public transient final static String sshParamKey = UserParametersFactoryBase.getPublicKeyParameterName();

    public SshCredential() {
    }

    public SshCredential(String publicKey) {
        this.publicKey = publicKey;
    }

    public SshCredential(Map<String, UserParameter> params) {
        setParams(params);
    }

    public String toJson() {
        return (new Gson()).toJson(this);
    }

    public static Object fromJson(String json, Class klass) {
        return (new Gson()).fromJson(json, klass);
    }

    @Override
    public boolean equalsTo(ISshCredential<T> other) {
        return credEquals(other);
    }

    private Form queryParameters() {

        // search for last modified

        String filter = "type='ssh-public-key'";
        Form queryParameters = new Form();
        queryParameters.add("$filter", filter);
        queryParameters.add("$orderby", "updated:desc");
        queryParameters.add("$last", "1");

        return queryParameters;
    }

    /**
     * This will persist only class attributes.  No credential parameters
     * from User will be persisted.
     * <p>
     * search (order by created, take last) / merge / create new / delete old
     */
    public void store(User user) throws ValidationException {
        String authz = user.getName() + " USER";

        List<SshCredential> credList = searchCollection(user);
        if (credList.isEmpty()) {
            // Create
            // TODO: use factory to provide CredentialCreateTmpl for Ssh and Cloud
            SscljProxy.post(SscljProxy.CREDENTIAL_RESOURCE, authz, new SshCredentialCreateTmpl(this), true);
        } else {
            // Edit

            // merge
            SshCredential credOld = credList.get(0);
            merge(credOld);

            // create new
            // TODO: use factory to provide CredentialCreateTmpl for Ssh and Cloud
            SscljProxy.post(SscljProxy.CREDENTIAL_RESOURCE, authz, new SshCredentialCreateTmpl(this), true);

            // delete old
            SscljProxy.delete(SscljProxy.BASE_RESOURCE + credOld.id, authz, true);
        }
    }

    private List<SshCredential> searchCollection(User user) {
        String authz = user.getName() + " USER";
        Response resp = SscljProxy.get(SscljProxy.CREDENTIAL_RESOURCE, authz, queryParameters());
        SshCredentialCollection cc = (SshCredentialCollection) fromJson(resp.getEntityAsText(),
                SshCredentialCollection.class);
        return cc.getCredentials();
    }

    public void load(User user) throws ValidationException {
        List<SshCredential> credList = searchCollection(user);
        if (!credList.isEmpty()) {
            merge(credList.get(0));
        }
    }

    public void removeAll(User user) {
        List<SshCredential> credList = searchCollection(user);
        if (!credList.isEmpty()) {
            for (SshCredential cred: credList) {
                delete(cred, user);
            }
        }

    }

    public static void delete(SshCredential cred, User user) {
        String authz = user.getName() + " USER";
        SscljProxy.delete(SscljProxy.BASE_RESOURCE + cred.id, authz);
    }

    @Override
    public ISshCredential fromJson(String json) {
        return null;
    }

    @Override
    public boolean credEquals(ISshCredential<T> other) {
        return false;
    }

    public void merge(ISshCredential<T> other) throws ValidationException {
        Map<String, UserParameter> otherParams = other.getParams();
        otherParams.putAll(getParams());
        setParams(otherParams);
    }

    @Override
    public void setParams(Map<String, UserParameter> params) {
        if (params.containsKey(sshParamKey)) {
            publicKey = params.get(sshParamKey).getValue();
        }
    }

    @Override
    public Map<String, UserParameter> getParams() throws ValidationException {
        Map<String, UserParameter> params = new HashMap<>();

        if (null != this.publicKey) {
            UserParameter pubKey = new UserParameter(sshParamKey, this.publicKey,
                    "SSH Public Key(s) (one per line)");
            pubKey.setInstructions("Warning: Some clouds may take into account only the first key until SlipStream bootstraps the machine.");
            params.put(sshParamKey, pubKey);
        }
        return params;
    }
}
