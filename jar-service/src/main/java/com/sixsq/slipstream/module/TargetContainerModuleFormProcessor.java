package com.sixsq.slipstream.module;

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
 *  -=================================================================-
 */


import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Target;
import com.sixsq.slipstream.persistence.TargetContainerModule;
import com.sixsq.slipstream.persistence.User;
import org.restlet.data.Form;

import java.util.HashSet;
import java.util.Set;

public abstract class TargetContainerModuleFormProcessor extends ModuleFormProcessor  {

    public TargetContainerModuleFormProcessor(User user) {
        super(user);
    }

    @Override
    public void parseForm() throws ValidationException, NotFoundException {
        super.parseForm();

        parseTargets(getForm());
    }

    protected void parseTargets(Form form) throws ValidationException {

        Set<Target> targets = new HashSet<Target>();

        for (String targetName : Target.getTargetScriptNames()) {
            addTarget(form, targets, targetName);
        }

        castToModule().setTargets(targets);

    }

    protected void addTarget(Form form, Set<Target> targets, String targetName) {
        String target = form.getFirstValue(targetName + "--script");
        if (target != null) {
            targets.add(new Target(targetName, target));
        }
    }

    private TargetContainerModule castToModule() {
        return (TargetContainerModule) getParametrized();
    }

}
