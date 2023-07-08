package com.tapstream.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionInfo {
    private static final String version;

    static {
        String versionTemp = "Unknown";
        try (InputStream is = VersionInfo.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                versionTemp = props.getProperty("version");
            }

        } catch (IOException ignored) {
            
        }
        version = versionTemp;
    }

    public static String getVersion() {
        return version;
    }
}