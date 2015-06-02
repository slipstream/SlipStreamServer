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
 * <module> = project1/image1: shortName = image1 - id: uri - version:
 * <version> - parent:
 * 
 * Unit test see
 * 
 * @see ModuleUriUtilTest
 * 
 */
public class ModuleUriUtil {

	private static int DEFAULT_VERSION = Module.DEFAULT_VERSION;

	public static String extractShortNameFromResourceUri(String id) {
		String[] slices = id.split("/");
		int version = extractVersion(id);
		int index;
		String last = slices[slices.length - 1];
		if (version != DEFAULT_VERSION
				|| last.equals(Integer.toString(DEFAULT_VERSION))) {
			index = slices.length - 2;
		} else {
			index = slices.length - 1;
		}
		return slices[index];
	}

	public static String extractVersionLessResourceUri(String id)
			throws ValidationException {
		String parentUri = extractParentUriFromResourceUri(id);
		String shortName = extractShortNameFromResourceUri(id);
		String sep = parentUri.endsWith("/") ? "" : "/";
		return parentUri + sep + shortName;
	}

	private static int extractVersion(String id) {
		String[] slices = id.split("/");
		String last = slices[slices.length - 1];
		int version = DEFAULT_VERSION;
		try {
			version = Integer.parseInt(last);
		} catch (NumberFormatException e) {
		}
		return version;
	}

	public static int extractVersionFromResourceUri(String id) {
		return extractVersion(id);
	}

	public static String extractModuleUriFromResourceUri(String id) {
		return id;
	}

	public static String extractModuleNameFromResourceUri(String id) {

		if (Module.RESOURCE_URI_PREFIX.replace("/", "").equals(id)
				|| Module.RESOURCE_URI_PREFIX.equals(id)) {
			return "";
		}

		Integer version = extractVersion(id);
		int startIndex = (id.startsWith(Module.RESOURCE_URI_PREFIX) ? Module.RESOURCE_URI_PREFIX
				.length() : 0);

		int endIndex;
		String defaultVersionPostFix = "/" + Module.DEFAULT_VERSION;
		if (id.endsWith(defaultVersionPostFix)) {
			endIndex = id.length() - defaultVersionPostFix.length();
		} else {
			endIndex = (version != DEFAULT_VERSION) ? id.length()
					- version.toString().length() - 1 : id.length();
		}
		if (id.endsWith("/")) {
			endIndex = id.length() - 1;
		}

		return (endIndex <= startIndex ? "" : id.substring(startIndex,
				endIndex));
	}

	public static String extractParentUriFromResourceUri(String id)
			throws ValidationException {
		if (!id.startsWith(Module.RESOURCE_URI_PREFIX)) {
			throw (new ValidationException("Resource URI not starting with "
					+ Module.RESOURCE_URI_PREFIX));
		}

		String strippedResourceUri = stripDefaultVersionIfPresent(id);

		boolean hasParent = (id.split("/").length > 2);
		String moduleUrl = extractModuleUriFromResourceUri(strippedResourceUri);
		if (!hasParent) {
			return Module.RESOURCE_URI_PREFIX;
		}
		boolean hasVersion = (extractVersion(id) != -1);
		String moduleName = (hasVersion ? moduleUrl.substring(0,
				moduleUrl.lastIndexOf('/')) : strippedResourceUri);
		return moduleName.substring(0, moduleName.lastIndexOf('/'));
	}

	private static String stripDefaultVersionIfPresent(String id) {
		String defaultEnd = "/" + String.valueOf(DEFAULT_VERSION);
		String strippedResourceUri = id.endsWith(defaultEnd) ? id
				.substring(0, id.length() - defaultEnd.length())
				: id;
		return strippedResourceUri;
	}

}
