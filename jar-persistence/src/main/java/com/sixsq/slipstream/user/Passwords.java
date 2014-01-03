package com.sixsq.slipstream.user;

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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class Passwords {

	public String newPassword1 = null;
	public String newPassword2 = null;
	public String oldPassword = null;

	private Boolean hashed = false;

	public Passwords() {
	}

	public Passwords(String oldPassword, String newPassword1,
			String newPassword2) {
		this.newPassword1 = newPassword1;
		this.newPassword2 = newPassword2;
		this.oldPassword = oldPassword;
	}

	public void hash() throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		if (!hashed) {
			if (newPassword1 != null) {
				newPassword1 = hash(newPassword1);
			}
			if (newPassword2 != null) {
				newPassword2 = hash(newPassword2);
			}
			if (oldPassword != null) {
				oldPassword = hash(oldPassword);
			}
		}
	}

	public static String hash(String password) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		byte[] hash = md.digest(password.getBytes("UTF-8"));
		return (new HexBinaryAdapter()).marshal(hash);
	}

}
