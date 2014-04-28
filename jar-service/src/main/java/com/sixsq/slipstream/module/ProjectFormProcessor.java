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

import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ProjectModule;
import com.sixsq.slipstream.persistence.User;

public class ProjectFormProcessor extends ModuleFormProcessor {

	public ProjectFormProcessor(User user) {
		super(user);
	}

	@Override
	protected Module getOrCreateParameterized(String name) throws ValidationException {
		Module loaded = load(name);
		return loaded == null ? new ProjectModule(name) : loaded;
	}

}
