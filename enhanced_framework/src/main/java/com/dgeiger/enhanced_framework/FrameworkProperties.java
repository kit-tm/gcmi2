package com.dgeiger.enhanced_framework;

import java.io.*;
import java.util.Properties;

public class FrameworkProperties {

    private Properties properties;

    private static FrameworkProperties instance;
    private FrameworkProperties () {
        properties = new Properties();
    }

    void load(String configFilePath) throws IOException {
        File configFile = new File(configFilePath);
        InputStream input = new FileInputStream(configFile);
        properties.load(input);

        String[] stringPropertyKeys = {
                "upstreamIp", "gcmiApps"
        };

        String[] booleanPropertyKeys = {
                "useTls", "multipleProxies", "useCache", "saveTimestamps", "matchAgainstAtLeastOneFilter"
        };

        String[] integerPropertyKeys = {
                "upstreamPort", "downstreamPort"
        };

        for(String key : stringPropertyKeys){
            if(!isNotEmpty(key)){
                throw new IllegalArgumentException("Invalid property for key " + key);
            }
        }

        for(String key : booleanPropertyKeys){
            if(!isValidBoolean(key))
                throw new IllegalArgumentException("Invalid boolean value for key " + key);
        }

        for(String key : integerPropertyKeys){
            if(!isValidInteger(key))
                throw new IllegalArgumentException("Invalid integer value for key " + key);
        }
    }

    private boolean isNotEmpty(String key){
        return !properties.getProperty(key).isEmpty();
    }

    private boolean isValidBoolean(String key){
        return properties.getProperty(key).equalsIgnoreCase("true")
                || properties.getProperty(key).equalsIgnoreCase("false");
    }

    private boolean isValidInteger(String key){
        try {
            Integer.parseInt(properties.getProperty(key));
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static int getInteger(String key){
        return Integer.parseInt(getInstance().getProperty(key));
    }

    public static String getString(String key){
        return getInstance().getProperty(key);
    }

    private String getProperty(String key){
        return properties.getProperty(key);
    }

    public static boolean getBoolean(String key){
        return Boolean.parseBoolean(getInstance().getProperty(key));
    }

    public static String[] getStringArray(String key){
        return getInstance().getProperty(key).split(",");
    }

    static synchronized FrameworkProperties getInstance() {
        if (instance == null) {
            instance = new FrameworkProperties ();
        }
        return instance;
    }
}
