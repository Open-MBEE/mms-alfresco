package gov.nasa.jpl.view_repo.util;

import java.io.InputStream;
import java.util.Properties;

public class EmsConfig {
    public static Properties properties = new Properties();

    public static void setProperties(String propertiesFile) throws Exception {
        InputStream resourceAsStream = EmsConfig.class.getClassLoader().getResourceAsStream("mms.properties");

        if (resourceAsStream != null) {
            properties.load(resourceAsStream);
        } else {
            EmsConfig.properties
            .load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFile));
        }
    }

    public static void setAlfrescoProperties(Properties alfrescoProperties) throws Exception {
        EmsConfig.properties.putAll(alfrescoProperties);
    }

    public static String get(String key) {
        return EmsConfig.properties.getProperty(key);
    }

    public static void setProperty(String key, String value) {
        EmsConfig.properties.setProperty(key, value);
    }
}
