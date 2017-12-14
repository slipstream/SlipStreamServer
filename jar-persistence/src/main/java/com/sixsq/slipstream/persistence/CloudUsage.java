package com.sixsq.slipstream.persistence;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

import org.simpleframework.xml.Attribute;


public class CloudUsage {

    @Attribute
    private String cloud;

    @Attribute(empty = "0")
    private Integer vmQuota = null;

    @Attribute(empty = "0")
    private Integer userRunUsage = null;

    public CloudUsage(String cloud) {
        this.cloud = cloud;
    }

    public CloudUsage(String cloud, Integer vmQuota, Integer userRunUsage)
    {
        this.cloud = cloud;
        this.vmQuota = vmQuota;
        this.userRunUsage = userRunUsage;
    }

    public String getCloud() {
        return this.cloud;
    }

    public Integer getVmQuota() {
        return this.vmQuota;
    }

    public void setVmQuota(Integer vmQuota) {
        this.vmQuota = vmQuota;
    }

    public Integer getUserRunUsage() {
        return userRunUsage;
    }

    public void setUserRunUsage(Integer userRunUsage) {
        this.userRunUsage = userRunUsage;
    }

    public void add(CloudUsage usage) {
        add(usage, true);
    }

    public void add(CloudUsage usage, boolean addQuota) {

        if (addQuota && usage.vmQuota != null) {
            if (this.vmQuota == null) {
                this.vmQuota = 0;
            }
            this.vmQuota += usage.vmQuota;
        }

        if (usage.userRunUsage != null) {
            if (this.userRunUsage == null) {
                this.userRunUsage = 0;
            }
            this.userRunUsage += usage.userRunUsage;
        }
    }

}
