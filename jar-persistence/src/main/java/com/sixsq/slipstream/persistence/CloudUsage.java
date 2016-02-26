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

    @Attribute
    private Integer quota = null;

    @Attribute
    private int userUsage = 0;

    @Attribute
    private int userInactiveUsage = 0;

    @Attribute
    private int othersUsage = 0;

    @Attribute
    private int pendingUsage = 0;

    @Attribute
    private int unknownUsage = 0;

    public CloudUsage(String cloud) {
        this.cloud = cloud;
    }

    public CloudUsage(String cloud, Integer quota, int userUsage, int userInactiveUsage, int othersUsage,
					  int pendingUsage, int unknownUsage) {
        this.cloud = cloud;
        this.quota = quota;
        this.userUsage = userUsage;
        this.userInactiveUsage = userInactiveUsage;
        this.othersUsage = othersUsage;
        this.unknownUsage = unknownUsage;
        this.pendingUsage = pendingUsage;
    }

    public String getCloud() {
        return this.cloud;
    }

    public Integer getQuota() {
        return this.quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public int getUserUsage() {
        return this.userUsage;
    }

    public void incrementUserUsage() {
        this.userUsage++;
    }

    public int getUserInactiveUsage() {
        return userInactiveUsage;
    }

    public void incrementUserInactiveUsage() {
        this.userInactiveUsage++;
    }

    public int getOthersUsage() {
        return othersUsage;
    }

    public void incrementOthersUsage() {
        this.othersUsage++;
    }

    public int getPendingUsage() {
        return pendingUsage;
    }

    public void incrementPendingUsage() {
        this.pendingUsage++;
    }

    public int getUnknownUsage() {
        return unknownUsage;
    }

    public void incrementUnknownUsage() {
        this.unknownUsage++;
    }

}
