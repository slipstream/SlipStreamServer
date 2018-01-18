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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.SlipStreamRuntimeException;
import com.sixsq.slipstream.exceptions.ValidationException;
import com.sixsq.slipstream.persistence.User;
import com.sixsq.slipstream.persistence.User.State;
import com.sixsq.slipstream.user.UserResource;
import com.sixsq.slipstream.util.Logger;

/*
** Utility class to create initial super user and load users from files
*/
public class Users {

	public static final String SUPER_USERNAME = "super";

    /**
     * Name of the directory containing user definition files.
     */
    private static final String USERS_CONFIG_DIR = "users";

    /**
     * Name of the directory containing password definition files.
     */
    private static final String PASSWORDS_CONFIG_DIR = "passwords";

    public static void create() throws ValidationException, NotFoundException,
			ConfigurationException, NoSuchAlgorithmException,
			UnsupportedEncodingException {
		createSuperUser();
	}

	private static void createSuperUser() throws ValidationException {
		User user = loadSuper();
		if (user != null) {
			return;
		}
		user = createUser(SUPER_USERNAME);
		user.setFirstName("Super");
		user.setLastName("User");
		user.setEmail("super@sixsq.com");
		user.setOrganization("SixSq");
		try {
			user.hashAndSetPassword("supeRsupeR");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new ValidationException(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new ValidationException(e.getMessage());
		}
		user.setState(State.ACTIVE);

        user = loadPassword(user);

		user = user.store();

		// It's forbidden to create super user directly.
		user.setSuper(true);
		user.store();
	}

    public static User loadSuper() throws ValidationException {
		return User.loadByNameNoParams(SUPER_USERNAME);
	}

	private static User createUser(String name) {
		User user = null;
		try {
			user = new User(name);
		} catch (ValidationException e) {
			throw (new SlipStreamRuntimeException(e));
		}
		return user;
	}

    public static void load() {

		if (!shouldLoad()) {
			return; // users have already been loaded or altered by admin
		}

		// load from file
		File configDir = Configuration.findConfigurationDirectory();
		if(configDir == null) {
			return;
		}

		File usersDir = new File(configDir + File.separator + USERS_CONFIG_DIR);

		List<File> files = FileLoader.loadConfigurationFiles(usersDir);
		files.forEach( f -> { loadSingleUser(f); } );
	}

	private static boolean shouldLoad() {
		return User.isSuperAlone();
	}

	public static void loadSingleUser(File f) {

        Logger.info("Loading config file: " + f.getPath());

		User user = null;
		try {
            user = UserResource.xmlToUser(FileLoader.fileToString(f));
        } catch (IOException e) {
            Logger.warning("Failed parsing user file: " + f.getPath() + " with error: " + e.getMessage());
        }
        File usersDir = new File(f.getParent());
        user = loadPasswordFile(user, getPasswordsDir(usersDir.getParent()));
		if(user != null) {
            user.store();
        }
	}

    private static File getPasswordsDir(String configDirPath) {
        return new File(configDirPath + File.separator + PASSWORDS_CONFIG_DIR);
    }

    private static User loadPasswordFile(User user, File passwordsDir) {

        File file = findPasswordFile(user, passwordsDir);

        if(file == null) {
            return user; // no password file found
        }

        String password = null;
        Logger.info("Loading config file: " + file.getPath());
        try {
            password = FileLoader.fileToString(file).trim();
        } catch (IOException e) {
            Logger.warning("Failed parsing password file: " + file.getPath() + " with error: " + e.getMessage());
        }
        try {
            user.setPassword(password);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            Logger.warning("Failed setting password for user: " + user.getName() + " with error: " + e.getMessage());
        }

        return user;
    }

    private static File findPasswordFile(User user, File passwordsDir) {

        if(!passwordsDir.exists()) {
            return null;
        }

        File[] files = passwordsDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.equals(user.getName());
                }
            });
        return (files.length == 0) ? null : files[0];
    }

    private static User loadPassword(User user) {
        File configDir = Configuration.findConfigurationDirectory();

        if(configDir == null) {
            return user;
        }

        File passwordDir = getPasswordsDir(configDir.getAbsolutePath());

        return loadPasswordFile(user, passwordDir);

    }

}
