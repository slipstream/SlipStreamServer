package com.sixsq.slipstream.connector;

import com.sixsq.slipstream.es.CljElasticsearchHelper;
import com.sixsq.slipstream.ssclj.app.SscljTestServer;
import com.sixsq.slipstream.util.UserTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConnectorFactoryTest {

    @BeforeClass
    public static void setupClass() {
        SscljTestServer.start();
        CljElasticsearchHelper.initTestDb();
        UserTestUtil.setCloudConnector("");
    }

    @AfterClass
    public static void teardownClass() {
        SscljTestServer.stop();
    }

    @After
    public void afterTest() {
        UserTestUtil.setCloudConnector("");
    }

    @Test
    public void cloudNameFromInstanceNameTest() {
        UserTestUtil.setCloudConnector("c1, c1-instance1:c1 , c2");
        assertNull(ConnectorFactory.cloudNameFromInstanceName(""));
        assertNull(ConnectorFactory.cloudNameFromInstanceName(" "));
        assertNull(ConnectorFactory.cloudNameFromInstanceName("does-not-exist"));
        assertTrue("c1".equals(ConnectorFactory.cloudNameFromInstanceName("c1-instance1")));
        assertTrue("c1".equals(ConnectorFactory.cloudNameFromInstanceName("c1")));
        assertTrue("c2".equals(ConnectorFactory.cloudNameFromInstanceName("c2")));
    }
}
