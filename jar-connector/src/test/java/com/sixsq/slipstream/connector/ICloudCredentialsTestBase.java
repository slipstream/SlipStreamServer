package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.UserParameter;

import java.util.Map;

public interface ICloudCredentialsTestBase {
    String getCloudServiceName();
    SystemConfigurationParametersFactoryBase getSystemConfParams()
            throws ValidationException;
    Map<String, UserParameter> createAndStoreCloudCredentials()
            throws ValidationException;
}
