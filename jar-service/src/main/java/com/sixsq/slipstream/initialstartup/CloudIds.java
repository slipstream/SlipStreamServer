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
import com.sixsq.slipstream.persistence.ImageModule;
import com.sixsq.slipstream.persistence.Module;
import com.sixsq.slipstream.persistence.ModuleCategory;
import com.sixsq.slipstream.util.Logger;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

/*
** Utility class to load initial modules from files
*/
public class CloudIds {

    /**
     * Name of the directory containing module definition files.
     */
    private static final String CLOUD_IDS_CONFIG_DIR = "cloud-ids";

    public static void load() {

		if (!shouldLoad()) {
			return; // modules have already been loaded
		}

		// load from file
		File configDir = Configuration.findConfigurationDirectory();
		if(configDir == null) {
			return;
		}

		File usersDir = new File(configDir + File.separator + CLOUD_IDS_CONFIG_DIR);

		List<File> files = FileLoader.loadConfigurationFiles(usersDir);
		files.forEach( f -> { loadSingleCloudIds(f); } );
	}

	private static boolean shouldLoad() {

		return true; // we always read the files and then check if the ids are set
	}

	private static void loadSingleCloudIds(File f) {

		Logger.info("Loading config file: " + f.getPath());

		Properties props = Configuration.loadPropertiesFile(f.toURI(), new Properties());
		for (Entry entry : props.entrySet()) {
			String imageUri = (String) entry.getKey();
			String[] values = ((String)entry.getValue()).split(":");
			if(values.length != 2) {
				Logger.warning("Error loading cloud id file: " + f.getPath() + " reading image: " + imageUri + ". Value should be in to form: <cloud-id>:<unique-id>");
                continue;
			}
			String cloudName = values[0];
			String cloudId = values[1];
            Module module = Module.loadLatest(Module.RESOURCE_URI_PREFIX + imageUri);
            if(module == null) {
                Logger.warning("Error processing file: " + f.getPath() + ". Module: " + imageUri + " does not exist.");
            }
            if(module.getCategory() != ModuleCategory.Image) {
                Logger.warning("Error processing file: " + f.getPath() + ". Module: " + imageUri + " is not an image.");
            }
            ImageModule image = (ImageModule)ImageModule.loadLatest(Module.RESOURCE_URI_PREFIX + imageUri);
			if (image.getCloudImageId(cloudName) != null) {
				image.setImageId(cloudId, cloudName);
				image.store(false);
			}
		}
	}

}
