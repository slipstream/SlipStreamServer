package com.sixsq.slipstream.credentials;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

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
public class SshCredentials {

    private static final Gson gson = new Gson();

    private List<ISshCredential> credentials = new ArrayList<>();
    private Integer count;

    public List<ISshCredential> getCredentials() {
        return credentials;
    }

    public Integer getCount() {
        return count;
    }

    public static Object fromJson(String jsonRecords, Class klass) {
        return gson.fromJson(jsonRecords, klass);
    }
}
