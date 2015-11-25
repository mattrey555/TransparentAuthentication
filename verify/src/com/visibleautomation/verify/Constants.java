package com.visibleautomation.verify;
import java.io.IOException;
import java.io.*;
import java.util.Properties;

public class Constants {
    //public static final String DB_CONNECTION = "jdbc:mysql://localhost:3306/transparent_authentication";
    public static final String DB_CONNECTION_FORMAT = "jdbc:mysql://localhost:3306/%s";
    private static final String SQL_PROPERTIES_FILE = "sql.properties";
    private static final String SQL_USERNAME_PROPERTY = "sql.username";
    private static final String SQL_PASSWORD_PROPERTY = "sql.password";
    private static final String SQL_DATABASE_PROPERTY = "sql.database";
    public static String sdbUsername = null;
    public static String sdbPassword = null;
    public static String sdbDatabase = null;
    private static final String GCM_PROPERTIES_FILE = "gcm.properties";
    private static final String GCM_SERVER_PROPERTY = "gcm.server";
    private static final String GCM_PORT_PROPERTY = "gcm.port";
    private static final String GCM_PROJECTID_PROPERTY = "gcm.projectid";
    private static final String GCM_APIKEY_PROPERTY = "gcm.apikey";
    public static String sGCMServer = null;
    public static String sGCMPort = null;
    public static String sGCMProjectId = null;
    public static String sGCMApiKey = null;

    public static void setDatabaseVariables() {
        Properties sqlProperties = new Properties();
        InputStream is = null;
        try {   
            is = Constants.class.getClassLoader().getResourceAsStream(SQL_PROPERTIES_FILE);
            if (is != null) {
                sqlProperties.load(is);
                sdbUsername = sqlProperties.getProperty(SQL_USERNAME_PROPERTY);
                sdbPassword = sqlProperties.getProperty(SQL_PASSWORD_PROPERTY);
                sdbDatabase = sqlProperties.getProperty(SQL_DATABASE_PROPERTY);
                System.out.println("Verify: sql.properties file loaded successfully username = " + 
                                   sdbUsername + " password = " + sdbPassword + " database = " + sdbDatabase);
            } else {
                System.out.println("failed to load " + SQL_PROPERTIES_FILE);
            }
        } catch (IOException ioex) {
            System.out.println("Verify: failed to load properties file " + SQL_PROPERTIES_FILE + "message = " + ioex.getMessage());
        } catch (Exception ex) {
            System.out.println("Verify: failed to load properties file " + SQL_PROPERTIES_FILE + "message = " + ex.getMessage());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioex) {
            }
        }
    }

    public static void setGCMVariables() {
        InputStream is = null;
        Properties gcmProperties = new Properties();
        try {   
            is = Constants.class.getClassLoader().getResourceAsStream(GCM_PROPERTIES_FILE);
            if (is != null) {
                gcmProperties.load(is);
                sGCMServer = gcmProperties.getProperty(GCM_SERVER_PROPERTY);
                sGCMPort = gcmProperties.getProperty(GCM_PORT_PROPERTY);
                sGCMProjectId = gcmProperties.getProperty(GCM_PROJECTID_PROPERTY);
                sGCMApiKey = gcmProperties.getProperty(GCM_APIKEY_PROPERTY);
                System.out.println("Verify: gcm.properties file loaded successfully server = " + 
                                   sGCMServer + " port = " + sGCMPort + " projectId = " + sGCMProjectId + " apikey = " + sGCMApiKey);
            } else {
                System.out.println("failed to load " + GCM_PROPERTIES_FILE);
            }
        } catch (IOException ioex) {
            System.out.println("Verify: failed to load properties file " + GCM_PROPERTIES_FILE + "message = " + ioex.getMessage());
        } catch (Exception ex) {
            System.out.println("Verify: failed to load properties file " + GCM_PROPERTIES_FILE + "message = " + ex.getMessage());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioex) {
            }
        }
    }
}
