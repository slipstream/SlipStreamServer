package com.sixsq.slipstream.persistence;

import com.google.gson.Gson;
import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.connector.ExecutionControlUserParametersFactory;
import com.sixsq.slipstream.connector.UserParametersFactoryBase;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.ssclj.util.UserParamsDesc;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserGeneralParams {
    public Integer timeout = 30;
    public Integer verbosityLevel = Integer.parseInt(ExecutionControlUserParametersFactory.VERBOSITY_LEVEL_DEFAULT);
    public String defaultCloudService = "";
    public String keepRunning = UserParameter.KEEP_RUNNING_DEFAULT;
    public String mailUsage = UserParameter.MAIL_USAGE_DEFAULT;
    public String sshPublicKey = "";
    public String id;
    public String paramsType = "execution";

    public UserGeneralParams() { }

    public UserGeneralParams(String defaultCloudService, String keepRunning,
                             String mailUsage, Integer timeout, Integer
                                     verbosityLevel, String sshPublicKey) {
        this.defaultCloudService = defaultCloudService;
        this.keepRunning = keepRunning;
        this.mailUsage = mailUsage;
        this.timeout = timeout;
        this.verbosityLevel = verbosityLevel;
        this.sshPublicKey = sshPublicKey;
    }

    public static UserGeneralParams fromJson(String jsonRecords) {
        return (new Gson()).fromJson(jsonRecords, UserGeneralParams.class);
    }

    public void setParameter(UserParameter param) {
        String pName = param.getName();
        String pValue = param.getValue();
        String category = param.getCategory();
        if (pName.equals(UserParameter.constructKey(category,
                UserParameter.KEY_KEEP_RUNNING))) {
            this.keepRunning = pValue;
        } else if (pName.equals(UserParameter.constructKey(category,
                UserParameter.KEY_MAIL_USAGE))) {
            this.mailUsage = pValue;
        } else if (pName.equals(UserParameter.constructKey(category,
                ExecutionControlUserParametersFactory.VERBOSITY_LEVEL))) {
            this.verbosityLevel = Integer.parseInt(pValue);
        } else if (pName.equals(UserParameter.constructKey(category,
                UserParameter.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME))) {
            this.defaultCloudService = pValue;
        } else if (pName.equals(UserParameter.constructKey(category,
                UserParameter.KEY_TIMEOUT))) {
            this.timeout = Integer.parseInt(pValue);
        } else if (pName.equals(UserParameter.constructKey(category,
                UserParameter.SSHKEY_PARAMETER_NAME))) {
            this.sshPublicKey = pValue;
        }
    }
    public void setParameters(Collection<UserParameter> params) {
        for (UserParameter p: params) {
           setParameter(p);
        }
    }

    public Map<String, UserParameter> toParameters() throws ValidationException {
        Map<String, UserParameter> params = new HashMap<>();
        Map<String, Map> paramsDesc = UserParamsDesc.getExecDesc();

        String category = ParameterCategory.General.toString();
        UserParameter param;
        String k;

        k = UserParameter.constructKey(category, UserParameter.KEY_KEEP_RUNNING);
        param = new UserParameter(k, this.keepRunning, "");
        param.setDescription((String) paramsDesc.get("keepRunning").get("description"));
        param.setInstructions((String) paramsDesc.get("keepRunning").get("instructions"));
        param.setCategory(category);
        params.put(k, param);

        k = UserParameter.constructKey(category, UserParameter.KEY_MAIL_USAGE);
        param = new UserParameter(k, this.mailUsage, "");
        param.setDescription((String) paramsDesc.get("mailUsage").get("description"));
        param.setInstructions((String) paramsDesc.get("mailUsage").get("instructions"));
        param.setCategory(category);
        params.put(k, param);

        k = UserParameter.constructKey(category, ExecutionControlUserParametersFactory.VERBOSITY_LEVEL);
        param = new UserParameter(k, Integer.toString(this.verbosityLevel), "");
        param.setDescription((String) paramsDesc.get("verbosityLevel").get("description"));
        param.setInstructions((String) paramsDesc.get("verbosityLevel").get("instructions"));
        param.setCategory(category);
        params.put(k, param);

        k = UserParameter.constructKey(category, UserParameter.KEY_TIMEOUT);
        param = new UserParameter(k, Integer.toString(this.timeout), "");
        param.setDescription((String) paramsDesc.get("timeout").get("description"));
        param.setInstructions((String) paramsDesc.get("timeout").get("instructions"));
        param.setCategory(category);
        params.put(k, param);

        k = UserParameter.constructKey(category, UserParameter.DEFAULT_CLOUD_SERVICE_PARAMETER_NAME);
        param = new UserParameter(k, this.defaultCloudService, "");
        param.setDescription((String) paramsDesc.get("defaultCloudService").get("description"));
        param.setInstructions((String) paramsDesc.get("defaultCloudService").get("instructions"));
        param.setCategory(category);
        List<String> clouds = UserParametersFactoryBase.extractCloudNames(ConnectorFactory.getConnectors());
        param.setEnumValues(clouds);
        params.put(k, param);

        k = UserParameter.constructKey(category, UserParameter.SSHKEY_PARAMETER_NAME);
        param = new UserParameter(k, this.sshPublicKey, "");
        param.setDescription((String) paramsDesc.get("sshPublicKey").get("description"));
        param.setInstructions((String) paramsDesc.get("sshPublicKey").get("instructions"));
        param.setCategory(category);
        params.put(k, param);

        return params;
    }
}
