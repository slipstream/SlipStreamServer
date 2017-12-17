package com.sixsq.slipstream.resource;

import com.sixsq.slipstream.module.ModuleView.ModuleViewList;
import com.sixsq.slipstream.module.ModuleListResource;
import com.sixsq.slipstream.persistence.Module;

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

public class ModulesChooserResource extends ModuleListResource {

	@Override
	protected ModuleViewList retrieveFilteredModuleViewList() {

		ModuleViewList modules = new ModuleViewList(Module.viewList("module/"));
		ModuleViewList published = new ModuleViewList(Module.viewPublishedList());
		modules.getList().addAll(published.getList());
		return filterAuthz(modules);
	}

	@Override
	protected String getPageRepresentation() {
		return "chooser";
	}

}
