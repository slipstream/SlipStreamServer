package com.sixsq.slipstream.util;

import java.util.List;

import com.sixsq.slipstream.module.ModuleView;
import com.sixsq.slipstream.persistence.Module;

public abstract class ModuleTestUtil {

	public static void cleanupModules() {
		List<ModuleView> moduleViewList = Module
				.viewList(Module.RESOURCE_URI_PREFIX);
		for (ModuleView m : moduleViewList) {
			Module.loadByName(m.getName()).remove();
		}
	}

}
