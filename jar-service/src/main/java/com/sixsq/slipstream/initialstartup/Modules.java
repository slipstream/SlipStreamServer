package com.sixsq.slipstream.initialstartup;

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

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.module.ModuleResource;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/*
** Utility class to load initial modules from files
*/
public class Modules {

    /**
     * Name of the directory containing module definition files.
     */
    private static final String MODULES_CONFIG_DIR = "modules";

    public static void load() {

		if (!shouldLoad()) {
			return; // modules have already been loaded
		}

		// load from file
		File configDir = Configuration.findConfigurationDirectory();
		if(configDir == null) {
			return;
		}

		File usersDir = new File(configDir + File.separator + MODULES_CONFIG_DIR);

		List<File> files = FileLoader.loadConfigurationFiles(usersDir);
		files.forEach( f -> { loadSingleModule(f); } );
	}

	private static boolean shouldLoad() {
		return Module.isEmpty();
	}

	private static void loadSingleModule(File f) {

		Logger.info("Loading config file: " + f.getPath());

		Module module = null;
		try {
            module = ModuleResource.xmlToModule(FileLoader.fileToString(f));
        } catch (IOException e) {
            Logger.warning("Failed parsing module file: " + f.getPath() + " with error: " + e.getMessage());
        }
		if(module != null) {
            module.store();
        }
	}

}
