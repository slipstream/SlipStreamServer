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

import com.sixsq.slipstream.exceptions.ValidationException;
import org.simpleframework.xml.ElementList;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class TargetContainerModule extends Module {

    @ElementList(required = false)
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    protected Set<Target> targets = new HashSet<Target>();

    protected TargetContainerModule() {
        super();
    }

    protected TargetContainerModule(String name, ModuleCategory category) throws ValidationException {
        super(name, category);
    }

    public Set<Target> getTargets() {
        return targets;
    }

    public void setTargets(Set<Target> targets) {
        this.targets.clear();
        for (Target t : targets) {
            setTarget(t);
        }
    }

    private void setTarget(Target target) {
        target.setModule(this);
        targets.add(target);
    }

    public void setModuleToTargets() {
        if (targets != null) {
            for (Target t : targets) {
                t.setModule(this);
            }
        }
    }

}
