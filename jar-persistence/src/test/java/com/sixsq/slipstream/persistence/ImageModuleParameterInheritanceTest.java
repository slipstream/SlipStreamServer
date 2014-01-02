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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;

import org.junit.Test;

import com.sixsq.slipstream.connector.InstanceType;
import com.sixsq.slipstream.exceptions.ValidationException;

public class ImageModuleParameterInheritanceTest {

	private final String PARAMETER_NAME = "param_name";
	
	@Test
	public void parameterValueWithValue() throws ValidationException {
		ImageModule image = new ImageModule("image");
		
		ModuleParameter parameter = new ModuleParameter(PARAMETER_NAME);
		parameter.setValue("some value");

		image.setParameter(parameter);
		
		assertThat(image.getParameter(PARAMETER_NAME).getValue(), is("some value"));
	}

	@Test
	public void inheritedParameterValue() throws ValidationException {

		ImageModule baseImage = new ImageModule("base");
		ModuleParameter baseParameter = new ModuleParameter(PARAMETER_NAME);
		baseParameter.setValue("some value");
		baseImage.setParameter(baseParameter);
		baseImage.setIsBase(true);
		baseImage.store();
		
		ImageModule image = new ImageModule("image");
		ModuleParameter parameter = baseParameter.copy();
		image.setParameter(parameter);
		image.setModuleReference(baseImage);
		
		assertThat(image.getParameter(PARAMETER_NAME).getValue(), is("some value"));
		
		baseImage.remove();
	}

	@Test
	public void inheritedParameterWithDefaultInParentValue() throws ValidationException {
		ImageModule image = new ImageModule("image");
		
		ModuleParameter parameter = new ModuleParameter(PARAMETER_NAME);
				
		image.setParameter(parameter);

		ImageModule baseImage = new ImageModule("base");
		ModuleParameter baseParameter = parameter.copy();
		baseParameter.setValue("some default");
		baseImage.setParameter(baseParameter);
		baseImage.store();
		
		image.setModuleReference(baseImage);
		
		assertThat(image.getParameter(PARAMETER_NAME).getValue(), is("some default"));
		
		baseImage.remove();
	}

	@Test
	public void inheritedEnumParameterValue() throws ValidationException {
		ImageModule image = new ImageModule("image");
		
		ModuleParameter parameter = new ModuleParameter(PARAMETER_NAME);
		parameter.setType(ParameterType.Enum);
		parameter.setEnumValues(Arrays.asList(InstanceType.INHERITED, "A", "B"));
		parameter.setValue(InstanceType.INHERITED);
		image.setParameter(parameter);

		ImageModule baseImage = new ImageModule("base");
		ModuleParameter baseParameter = parameter.copy();
		baseParameter.setValue("A");
		baseImage.setParameter(baseParameter);
		baseImage.store();
		
		image.setModuleReference(baseImage);
		
		assertThat(image.getParameter(PARAMETER_NAME).getValue(), is("A"));
		
		baseImage.remove();
	}

}