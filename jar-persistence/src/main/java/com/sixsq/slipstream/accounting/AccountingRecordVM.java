package com.sixsq.slipstream.accounting;

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
public class AccountingRecordVM {

    private Integer cpu;
    private Integer ram;
    private Integer disk;
    private String instanceType;

    public AccountingRecordVM(Integer cpu, Integer ram, Integer disk, String instanceType) {
        this.cpu = cpu;
        this.ram = ram;
        this.disk = disk;
        this.instanceType = instanceType;
    }

    public Integer getCpu() {
        return cpu;
    }

    public Integer getRam() {
        return ram;
    }

    public Integer getDisk() {
        return disk;
    }

    public String getInstanceType() {return instanceType;}
}
