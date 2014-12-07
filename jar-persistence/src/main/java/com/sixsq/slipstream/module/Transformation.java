package com.sixsq.slipstream.module;

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.ElementList;

/**
 * Maintains transformation sequence. This sequence takes information from image
 * inheritance, such that a clear transformation sequence can be displayed and
 * process. Both build and deploy operations can be extracted from this
 * Transformation class, by parsing the stage ordered list.
 * 
 * Each stage is represented by a map of recipes , taken from one image in the
 * inheritance line. Recipes and packages can then be used to apply the
 * appropriate transformation.
 * 
 * The build property indicates if the image corresponding to the stage was
 * already built for the cloud concerned.
 * 
 * The moduleUri provides a link to the unique image module this stage
 * corresponds to.
 * 
 */
@SuppressWarnings("serial")
public class Transformation implements Serializable {

	@ElementList(inline = true, required = false)
	private List<Stage> stages = new ArrayList<Stage>();

	/**
	 * Known valid stages (aka types of recipes)
	 */
	enum Stages {
		prebuild, build, postbuild, predeploy, deploy, postdeploy, preonvmadd, onvmadd, postonvmadd, preonvmremove, onvmremove, postonvmremove
	}

	public List<Stage> getStages() {
		return stages;
	}

	public void setStages(List<Stage> stages) {
		this.stages = stages;
	}
}
