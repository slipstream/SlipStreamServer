package com.sixsq.slipstream.persistence;

import com.sixsq.slipstream.credentials.SshCredential;

import static org.junit.Assert.*;

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.ssclj.app.CIMITestServer;
import com.sixsq.slipstream.util.UserTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Ignore(value = "SSH keys were moved to user exec parameters.")
public class SshCredentialsTest {

    private User user;
    private final String sshParamKey = SshCredential.sshParamKey;
    private final String publicKey = "ssh-rsa ABCD x";

    @BeforeClass
    public static void setupClass() {
        CIMITestServer.start();
    }

    @AfterClass
    public static void teardownClass() {
        CIMITestServer.stop();
    }

    @Before
    public void before() throws ValidationException {
        user = UserTestUtil.createMinimalUser("test", "password");
        CIMITestServer.refresh();
    }

    @After
    public void after() {
        new SshCredential().removeAll(user);
        CIMITestServer.refresh();
    }

    @Test
    public void constructGetSetParamsTest() throws ValidationException {
        SshCredential cred = new SshCredential("foo");
        assertEquals("foo", cred.publicKey);
        Map<String, UserParameter> params = cred.getParams();
        assertTrue(params.containsKey(sshParamKey));
        assertTrue("foo".equals(params.get(sshParamKey).getValue()));
        assertTrue(!params.get(sshParamKey).getDescription().isEmpty());
        assertTrue(!params.get(sshParamKey).getInstructions().isEmpty());

        params.clear();
        assertEquals(0, params.size());
        UserParameter p = new UserParameter(sshParamKey, "bar", "");
        params.put(sshParamKey, p);
        cred.setParams(params);
        assertEquals("bar", cred.publicKey);
    }

    @Test
    public void storeLoadTest() throws ValidationException {
        SshCredential cred = new SshCredential((String) null);
        cred.load(user);
        assertEquals(null, cred.publicKey);

        SshCredential cred1 = new SshCredential(publicKey);
        cred1.store(user);
        SshCredential cred2 = new SshCredential((String) null);
        cred2.load(user);
        assertTrue(cred2.publicKey.startsWith(publicKey.substring(0, 13)));
    }

    @Test
    public void storeLoadMultiLinePubKeysTest() throws ValidationException {
        SshCredential cred1 = new SshCredential(publicKey + "\n" + publicKey + "\n");
        cred1.store(user);

        List<SshCredential> pubKeys = cred1.searchCollection(user);
        assertEquals(2, pubKeys.size());

        SshCredential cred2 = new SshCredential((String) null);
        cred2.load(user);
        assertTrue(cred2.publicKey.startsWith(publicKey.substring(0, 13)));
    }

    @Test
    public void storeLoadWithUserTest() throws ValidationException {
        UserParameter p;
        Map<String, UserParameter> params = new HashMap<>();

        p = user.getParameter(sshParamKey);
        assertEquals(null, p);

        p = new UserParameter(sshParamKey, publicKey, "");
        params.put(sshParamKey, p);
        user.setParameter(p);
        user.store();
        CIMITestServer.refresh();

        User u2 = User.loadByName(user.getName());
        p = u2.getParameter(sshParamKey);
        assertNotEquals(null, p);
        assertTrue(p.getValue().startsWith(publicKey.substring(0, 13)));
    }
}
