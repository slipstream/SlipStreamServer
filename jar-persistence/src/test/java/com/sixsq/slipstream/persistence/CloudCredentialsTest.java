package com.sixsq.slipstream.persistence;


import com.sixsq.slipstream.credentials.CloudCredential;
import com.sixsq.slipstream.credentials.CloudCredentialCollection;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class CloudCredentialsTest {

    public String jsonCloudCredentials() {
        ClassLoader classLoader = getClass().getClassLoader();
        String fName = "cloud-cred-test.json";
        URL file = classLoader.getResource(fName);
        if (null == file) fail("Failed to find file " + fName);
        String filePath = file.getFile();
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to load file with: " + e.getMessage());
            return "";
        }
    }

    @Test
    public void parseClodCredRecords() throws IOException {
        CloudCredentialCollection ccreds = (CloudCredentialCollection) CloudCredentialCollection.fromJson
                (jsonCloudCredentials(), CloudCredentialCollection.class);
        Assert.assertEquals(1, ccreds.getCredentials().size());

        CloudCredential ccred = (CloudCredential) ccreds.getCredentials().get(0);

        Assert.assertEquals("credential/1-2-3-4-5", ccred.id);
    }


}
