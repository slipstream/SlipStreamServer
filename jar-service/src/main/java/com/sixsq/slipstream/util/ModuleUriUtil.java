package com.sixsq.slipstream.util;

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

/**
 * Utilities to manipulate the resource URLs. The methods consistently do NOT
 * have a slash at the end of the returned URLs.
 * 
 * Terminology - uri: /module/<parent>/<module>/<version> - shortName: if
 * <module> = project1/image1: shortName = image1 - resourceUri: uri - version:
 * <version> - parent:
 * 
 * Unit test see
 * 
 * @see ModuleUriUtilTest
 * 
 */
public class ModuleUriUtil {

	private static int DEFAULT_VERSION = Module.DEFAULT_VERSION;

	public static String extractShortNameFromResourceUri(String resourceUri) {
		String[] slices = resourceUri.split("/");
		int version = extractVersion(resourceUri);
		int index;
		if (version != DEFAULT_VERSION) {
			index = slices.length - 2;
		} else {
			index = slices.length - 1;
		}
		return slices[index];
	}

	public static String extractVersionLessResourceUri(String resourceUri)
			throws ValidationException {
		String parentUri = extractParentUriFromResourceUri(resourceUri);
		String shortName = extractShortNameFromResourceUri(resourceUri);
		String sep = parentUri.endsWith("/") ? "" : "/";
		return parentUri + sep + shortName;
	}

	private static int extractVersion(String resourceUri) {
		String[] slices = resourceUri.split("/");
		String last = slices[slices.length - 1];
		int version = DEFAULT_VERSION;
		try {
			version = Integer.parseInt(last);
		} catch (NumberFormatException e) {
		}
		return version;
	}

	public static int extractVersionFromResourceUri(String resourceUri) {
		return extractVersion(resourceUri);
	}

	public static String extractModuleUriFromResourceUri(String resourceUri) {
		return resourceUri;
	}

	public static String extractModuleNameFromResourceUri(String resourceUri) {

		if (Module.RESOURCE_URI_PREFIX.replace("/", "").equals(resourceUri)
				|| Module.RESOURCE_URI_PREFIX.equals(resourceUri)) {
			return "";
		}

		Integer version = extractVersion(resourceUri);
		int startIndex = (resourceUri.startsWith(Module.RESOURCE_URI_PREFIX) ? Module.RESOURCE_URI_PREFIX
				.length() : 0);

		int endIndex;
		String defaultVersionPostFix = "/" + Module.DEFAULT_VERSION;
		if (resourceUri.endsWith(defaultVersionPostFix)) {
			endIndex = resourceUri.length() - defaultVersionPostFix.length();
		} else {
			endIndex = (version != DEFAULT_VERSION) ? resourceUri.length()
					- version.toString().length() - 1 : resourceUri.length();
		}
		if (resourceUri.endsWith("/")) {
			endIndex = resourceUri.length() - 1;
		}

		return (endIndex <= startIndex ? "" : resourceUri.substring(startIndex,
				endIndex));
	}

	public static String extractParentUriFromResourceUri(String resourceUri)
			throws ValidationException {
		if (!resourceUri.startsWith(Module.RESOURCE_URI_PREFIX)) {
			throw (new ValidationException("Resource URI not starting with "
					+ Module.RESOURCE_URI_PREFIX));
		}
		boolean hasParent = (resourceUri.split("/").length > 2);
		String moduleUrl = extractModuleUriFromResourceUri(resourceUri);
		if (!hasParent) {
			return Module.RESOURCE_URI_PREFIX;
		}
		boolean hasVersion = (extractVersion(resourceUri) != -1);
		String moduleName = (hasVersion ? moduleUrl.substring(0,
				moduleUrl.lastIndexOf('/')) : resourceUri);
		return moduleName.substring(0, moduleName.lastIndexOf('/'));
	}

}
