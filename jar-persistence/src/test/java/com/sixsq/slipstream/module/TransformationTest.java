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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.module.Transformation.Stages;
import com.sixsq.slipstream.persistence.Package;
import com.sixsq.slipstream.util.SerializationUtil;

public class TransformationTest {

	/**
	 * Example of json serialization:
{
  "stages": [{
    "built": false,
    "moduleUri": "moduleUri",
    "packages": [{
      "key": "key",
      "name": "package name",
      "repository": "repo"
    }],
    "recipes": {
      "prebuild": "prebuild recipe",
      "build": "build recipe"
    }
  }, {
    "built": false,
    "moduleUri": "parentModuleUri",
    "packages": [{
      "key": "key",
      "name": "package name",
      "repository": "repo"
    }],
    "recipes": {
      "prebuild": "prebuild recipe",
      "build": "build recipe"
    }
  }]
}
	 */
	@Test
	public void toJson() throws SlipStreamClientException {
		Transformation t = create();
		String json = SerializationUtil.toJsonString(t);
		t = (Transformation) SerializationUtil.fromJson(json, Transformation.class);

	}

	/**
	 * Example of xml serialization:
<transformation>
   <stage moduleUri="moduleUri" built="false">
      <recipes>
         <recipe name="prebuild"><![CDATA[prebuild recipe]]></recipe>
         <recipe name="build"><![CDATA[build recipe]]></recipe>
      </recipes>
      <package name="package name" repository="repo" key="key"/>
   </stage>
   <stage moduleUri="parentModuleUri" built="false">
      <recipes>
         <recipe name="prebuild"><![CDATA[prebuild recipe]]></recipe>
         <recipe name="build"><![CDATA[build recipe]]></recipe>
      </recipes>
      <package name="package name" repository="repo" key="key"/>
   </stage>
</transformation>
	 */
	@Test
	public void toXml() throws SlipStreamClientException {
		Transformation t = create();
		String xml = SerializationUtil.toXmlString(t);
		t = (Transformation) SerializationUtil.fromXml(xml, Transformation.class);
	}

	private Transformation create() throws ValidationException {
		Transformation t = new Transformation();

		List<Stage> stages = new ArrayList<Stage>();

		Map<Stages, String> recipes = new HashMap<Stages, String>();
		recipes.put(Stages.build, "build recipe");
		recipes.put(Stages.prebuild, "prebuild recipe");

		List<Package> packages = new ArrayList<Package>();
		packages.add(new Package("package name", "repo", "key"));

		stages.add(new Stage("moduleUri", recipes, packages));
		stages.add(new Stage("parentModuleUri", recipes, packages));

		t.setStages(stages);

		return t;
	}
}
