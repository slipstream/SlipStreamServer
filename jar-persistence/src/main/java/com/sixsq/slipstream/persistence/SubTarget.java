package com.sixsq.slipstream.persistence;

/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2014 SixSq Sarl (sixsq.com)
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

import com.sixsq.slipstream.exceptions.SlipStreamInternalException;
import org.simpleframework.xml.Attribute;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class SubTarget extends Target implements Serializable {

    @Attribute
    public final String moduleUri;

    @Attribute
    public final String moduleShortName;

    @Attribute
    public final int order;

    public SubTarget(Target target, int order) {
        if (target.module == null)
            throw new SlipStreamInternalException("Module has to be set when creating a SubTarget.");

        this.script = target.script;
        this.name = target.name;
        this.module = target.module;
        this.moduleUri = target.module.getResourceUri();
        this.moduleShortName = target.module.getShortName();
        this.order = order;
    }

    public SubTarget(Target target, int order, String moduleUri, String moduleShortName) {
        this.script = target.script;
        this.name = target.name;
        this.module = target.module;
        this.moduleUri = moduleUri;
        this.moduleShortName = moduleShortName;
        this.order = order;
    }

}