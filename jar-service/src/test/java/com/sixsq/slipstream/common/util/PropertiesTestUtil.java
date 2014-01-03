package com.sixsq.slipstream.common.util;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for accessing configuration file and building service url based
 * on configuration
 * 
 */

public class PropertiesTestUtil {

	public static final String systemPropertyName = "test.credentials.properties";

	public static final String defaultFilename = "test.credentials.properties";

	public static final Properties testProperties = new Properties();

	static {
		File file = getConfigurationFile();
		System.err.println("Using test configuration file: " + file);
		if (!file.canRead()) {
			System.err.println("ERROR: Test configuration file is unreadable.");
		}
		initializePropertiesFromFile(file);
	}

	private static File getConfigurationFile() {

		String name = System.getProperty(systemPropertyName);

		File file = new File((name != null) ? name : defaultFilename);
		file = file.getAbsoluteFile();

		return file;
	}

	private static void initializePropertiesFromFile(File file) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			testProperties.load(fis);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

	// Only static methods. Ensure no instances are created.
	private PropertiesTestUtil() {

	}

	/**
	 * Extract a named property from a given configuration file. Useful for
	 * testing.
	 * 
	 * @param propertyName
	 *            name of the property
	 * @return property value
	 * 
	 * @throws RuntimeException
	 *             if the property does not exist
	 */
	static public String getRequiredProperty(String propertyName) {
		String prop = PropertiesTestUtil.getOptionalProperty(propertyName);
		if (prop == null) {
			throw new RuntimeException("Missing configuration parameter: "
					+ propertyName);
		}
		return prop;
	}

	static public String getOptionalProperty(String propertyName) {
		String prop = testProperties.getProperty(propertyName);
		return prop;
	}

}
