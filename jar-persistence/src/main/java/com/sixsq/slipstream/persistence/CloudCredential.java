package com.sixsq.slipstream.persistence;


import com.google.gson.annotations.SerializedName;
import com.sixsq.slipstream.acl.ACL;
import com.sixsq.slipstream.acl.TypePrincipal;
import com.sixsq.slipstream.acl.TypePrincipalRight;
import com.sixsq.slipstream.util.SscljProxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sixsq.slipstream.acl.TypePrincipal.PrincipalType.USER;
import static com.sixsq.slipstream.acl.TypePrincipalRight.Right.ALL;

public class CloudCredential {


    private String instanceID;

    public String getInstanceID() {
        return instanceID;
    }
}
