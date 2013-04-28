package com.sixsq.slipstream.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtil {
	
	public static String fileToString(String file) {
        String result = null;
        DataInputStream in = null;

        try {
            File f = new File(file);
            byte[] buffer = new byte[(int) f.length()];
            in = new DataInputStream(new FileInputStream(f));
            in.readFully(buffer);
            result = new String(buffer);
        } catch (IOException e) {
            throw new RuntimeException("IO problem in fileToString", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) { }
        }
        return result;
    }

}
