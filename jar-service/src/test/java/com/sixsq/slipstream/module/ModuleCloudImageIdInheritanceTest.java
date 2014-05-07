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
 * -=================================================================-
 */

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.ImageModule;

public class ModuleCloudImageIdInheritanceTest {

	private static ImageModule baseImage;
	private static ImageModule builtImage;
	private static ImageModule notBuiltImage;
	private static ImageModule notBaseBaseImage; // with an image id set
	private static ImageModule notBaseImage; // without a base image id set
	private static ImageModule baseImageWithoutImageId; // without image id set

	private static String cloudName = "mycloud";

	@BeforeClass
	public static void setupClass() throws ValidationException,
			ConfigurationException {
		baseImage = new ImageModule("baseImage");
		baseImage.setImageId("baseId", cloudName);
		baseImage.setIsBase(true);
		baseImage = baseImage.store();

		notBaseBaseImage = new ImageModule("notBaseBaseImage");
		notBaseBaseImage.setImageId("notBaseBaseImageId", cloudName);
		notBaseBaseImage = notBaseBaseImage.store();

		notBaseImage = new ImageModule("notBaseImage");
		notBaseImage = notBaseImage.store();

		builtImage = new ImageModule("builtImage");
		builtImage.setModuleReference(baseImage);
		builtImage.setImageId("builtImageId", cloudName);
		builtImage = builtImage.store();

		notBuiltImage = new ImageModule("notBuiltImage");
		notBuiltImage.setModuleReference(baseImage);
		notBuiltImage.setRecipe("some recipe");
		notBuiltImage = notBuiltImage.store();

		baseImageWithoutImageId = new ImageModule("baseImageWithoutImageId");
		baseImageWithoutImageId.setIsBase(true);
		baseImageWithoutImageId = baseImageWithoutImageId.store();
	}

	@AfterClass
	public static void teardownClass() {
		baseImage.remove();
		builtImage.remove();
		notBuiltImage.remove();
		notBaseBaseImage.remove();
		notBaseImage.remove();
		baseImageWithoutImageId.remove();
		
	}

	@Test
	public void imageIdFromBase() throws ValidationException {
		assertThat(baseImage.extractBaseImageId(cloudName), is("baseId"));
	}

	@Test
	public void imageIdFromBuiltImage() throws ValidationException {
		assertThat(builtImage.extractBaseImageId(cloudName), is("builtImageId"));
	}

	@Test
	public void imageIdFromNotBuiltImage() throws ValidationException {
		assertThat(notBuiltImage.extractBaseImageId(cloudName), is("baseId"));
	}

	@Test
	public void imageIdFromNotBaseImageWithImageId() throws ValidationException {
		assertThat(notBaseBaseImage.extractBaseImageId(cloudName), is("notBaseBaseImageId"));
	}

	@Test(expected=ValidationException.class)
	public void imageIdFromBaseImageWithoutImageId() throws ValidationException {
		notBaseImage.extractBaseImageId(cloudName);
	}
	
	@Test(expected=ValidationException.class)
	public void baseImageWithoutImageId() throws ValidationException {
		baseImageWithoutImageId.extractBaseImageId(cloudName);
	}
	

}
