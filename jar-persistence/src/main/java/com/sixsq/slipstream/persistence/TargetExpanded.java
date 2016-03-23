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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


public class TargetExpanded implements Serializable {

    public enum BuildRecipe {
        PRE_RECIPE ("prerecipe"),
        RECIPE ("recipe");

        private String name = "";

        BuildRecipe(String name){
            this.name = name;
        }

        public String toString(){
            return name;
        }
    }

    @Attribute
    private String name;

    @ElementList(required = false, inline = true)
    protected Set<SubTarget> subTargets = new HashSet<>();

    public TargetExpanded(Target target) {
        this.name = target.name;

        TargetContainerModule module = target.getModule();

        findAndAddTargets(module);
    }

    public TargetExpanded(ImageModule image, BuildRecipe recipe) {
        this.name = recipe.toString();

        findAndAddBuildRecipeTargets(image, recipe);
    }

    private void findAndAddTargets(TargetContainerModule image) {
        findAndAddTargets(image, 1);
    }

    private int findAndAddTargets(TargetContainerModule module, int order) {

        if (module instanceof ImageModule) {
            ImageModule image = (ImageModule) module;
            ImageModule parent = image.getParentModule();
            if (parent != null)
                order = findAndAddTargets(parent, order);
        }

        Target target = findTarget(module, this.name);
        if (target != null) {
            this.subTargets.add(new SubTarget(target, order, module.getResourceUri(), module.getShortName()));
            order++;
        }

        return order;
    }

    private Target findTarget(TargetContainerModule module, String name) {
        for (Target t : module.getTargets()) {
            if (t.name != null && t.name.equals(name))
                return t;
        }
        return null;
    }

    private void findAndAddBuildRecipeTargets(ImageModule image, final BuildRecipe recipe) {
        findAndAddBuildRecipeTargets(image, recipe, 1);
    }

    private int findAndAddBuildRecipeTargets(ImageModule image, final BuildRecipe recipe, int order) {
        ImageModule parent = image.getParentModule();
        if (parent != null)
            order = findAndAddBuildRecipeTargets(parent, recipe, order);

        String script = null;
        if (recipe == BuildRecipe.PRE_RECIPE) {
            script = image.getPreRecipe();
        } else if (recipe == BuildRecipe.RECIPE) {
            script = image.getRecipe();
        }

        if (script != null) {
            this.subTargets.add(new SubTarget(new Target(this.name, script, image), order));
            order++;
        }

        return order;
    }

}
