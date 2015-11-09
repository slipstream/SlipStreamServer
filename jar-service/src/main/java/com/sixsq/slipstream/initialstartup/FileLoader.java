package com.sixsq.slipstream.initialstartup;

import com.sixsq.slipstream.connector.ConnectorFactory;
import com.sixsq.slipstream.exceptions.ConfigurationException;
import com.sixsq.slipstream.persistence.Parameter;
import com.sixsq.slipstream.persistence.ServiceConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class FileLoader {

    private static final String[] FILE_EXTENSIONS = {".conf", ".xml", ".json"};

    /**
     * Load configuration files, assumed to be in /etc/slipstream for
     * system installation.
     * @param configDir pointing to directory containing the config files
     * @return
     * @throws ConfigurationException
     */
    public static List<File> loadConfigurationFiles(File configDir) throws ConfigurationException {

        List<File> filteredFiles = new ArrayList<File>();

        if (configDir == null) {
            return filteredFiles;
        }
        if (!configDir.exists()) {
            return filteredFiles;
        }

        File[] files = configDir.listFiles();
        for (File f : files) {
            for (String ext : FILE_EXTENSIONS) {
                if(f.getName().endsWith(ext)) {
                    filteredFiles.add(f);
                }
            }
        }

        return filteredFiles;
    }

    public static String fileToString(File f) throws IOException {
        return new String(Files.readAllBytes(Paths.get(f.getPath())));
    }

}
