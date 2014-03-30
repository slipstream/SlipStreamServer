package com.sixsq.slipstream.messages;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;

public class XmlPropertiesResourceBundle extends ResourceBundle {

	private final Map<String, String> map;

	public XmlPropertiesResourceBundle(String basename) {

        InputStream in = null;
		try {

			Class<XmlPropertiesResourceBundle> c = XmlPropertiesResourceBundle.class;
			in = c.getResourceAsStream(basename);

			Properties properties = new Properties();
			properties.loadFromXML(in);
			map = transformPropertiesToMap(properties);

		} catch (InvalidPropertiesFormatException e) {
			throw new SlipStreamRuntimeException(e.getMessage());
		} catch (IOException e) {
			throw new SlipStreamRuntimeException(e.getMessage());
		} finally {
            if (in!=null) {
                try {
                    in.close();
                } catch (IOException consumed) {
                    // ignored
                }
            }
        }

	}

	@Override
	public Enumeration<String> getKeys() {
		return Collections.enumeration(map.keySet());
	}

	@Override
	protected Object handleGetObject(String key) {
		return map.get(key);
	}

	private static Map<String, String> transformPropertiesToMap(
			Properties properties) {

		Map<String, String> map = new HashMap<String, String>();

		for (Entry<Object, Object> entry : properties.entrySet()) {
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			map.put(key, value);
		}

		return Collections.unmodifiableMap(map);
	}

}
