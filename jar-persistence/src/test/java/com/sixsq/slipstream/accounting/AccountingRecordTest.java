package com.sixsq.slipstream.accounting;

import com.sixsq.slipstream.event.Event;
import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.*;
import com.sixsq.slipstream.statemachine.States;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by elegoff on 10.07.17.
 */
public class AccountingRecordTest {

    private static final Set<String> cloudServiceNames = new HashSet<String>(Arrays.asList("test"));



    @Test
    public void simpleStart()  throws ValidationException, NotFoundException, AbortException{
        {

            AccountingRecordHelper.unMuteForSomeTests();
            Event.muteForTests();
            ImageModule image = new ImageModule("test");
            image.setName("myImage");



            User user = new User("user");

            user.store();


            int nbRunBefore = Run.listAll().size();


            String expectedCloudName = "testcloudname";
            Set<String> cloudServiceNames = new HashSet<String>(Arrays.asList(expectedCloudName));

            Run provisioning = new Run(image, RunType.Run, cloudServiceNames, new User("user"));





            provisioning.setState(States.Provisioning);
            provisioning.store();

            int nbRunAfter = Run.listAll().size();
            assertThat(nbRunAfter, is(nbRunBefore + 1));



            provisioning.remove();


        }
    }



    @Test
    public void startAccountRecord() throws ValidationException, NotFoundException, AbortException {

        AccountingRecordHelper.unMuteForSomeTests();
        Event.muteForTests();
        ImageModule image = new ImageModule("test");
        image.setName("myImage");

        String nodeName = "myNode";
        //Node node = new Node(nodeName, image);

        User user = new User("user");
        user.setParameter(new UserParameter(UserParameter.KEY_TIMEOUT, "60", ""));
        user.store();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -10);
        Date tenMinAgo = calendar.getTime();

        int nbRunBefore = Run.listAll().size();


        String expectedCloudName = "testcloudname";
        Set<String> cloudServiceNames = new HashSet<String>(Arrays.asList(expectedCloudName));

        Run provisioning = new Run(image, RunType.Run, cloudServiceNames, new User("user"));
        provisioning.setStart(tenMinAgo);


        provisioning.assignRuntimeParameter("ss:groups", "k:v", "description");
        provisioning.addGroup("mygroup", "myServiceName");
        List<String> groups = provisioning.getGroupNameList();

        Assert.assertTrue("groups defintion found " +  groups, groups.contains("myServiceName:mygroup"));


        //provisioning.addNodeInstanceName(node, 1);

        //provisioning.composeNodeInstanceName(node, 1);
        String keyCloudService = RuntimeParameter.constructParamName(nodeName, RuntimeParameter.CLOUD_SERVICE_NAME);
        provisioning.assignRuntimeParameter(keyCloudService, expectedCloudName, "cloud name description");

        provisioning.setState(States.Provisioning);
        provisioning.store();

        int nbRunAfter = Run.listAll().size();
        assertThat(nbRunAfter, is(nbRunBefore + 1));



        provisioning.remove();


    }

    @Test
    public void runMutedProvisioningState() throws ValidationException {

        //This tests won't post accounting records
        Event.muteForTests();
        AccountingRecordHelper.muteForTests();

        Module image = new ImageModule("test");
        image.setName("myImage");

        User user = new User("user");
        user.setParameter(new UserParameter(UserParameter.KEY_TIMEOUT, "60", ""));
        user.store();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -10);
        Date tenMinAgo = calendar.getTime();

        int nbRunBefore = Run.listAll().size();

        Run provisioning = new Run(image, RunType.Run, cloudServiceNames, new User("user"));
        provisioning.setStart(tenMinAgo);
        provisioning.setState(States.Provisioning);
        provisioning.store();

        int nbRunAfter = Run.listAll().size();
        assertThat(nbRunAfter, is(nbRunBefore + 1));

        provisioning.remove();
    }

}
