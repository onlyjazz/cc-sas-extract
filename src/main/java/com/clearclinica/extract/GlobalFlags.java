package com.clearclinica.extract;// Singleton Design Pattern

public class GlobalFlags {
    public static String host = null;
    public static String port = null;
    public static String study_oid = null;
    public static String study_uid = null;
    public static String study_name = null;
    public static String study_id = null;
    public static String user = null;
    public static String password = null;
    public static String database = null;
    public static String mapFile = null;
    public static String xmlFile = null;
    public static boolean nomap = false;
    public static boolean spss = false;
    public static boolean output_to_file = false;

    private GlobalFlags() {
    }

    static private GlobalFlags _instance;

    static public GlobalFlags getInstance() {
        if (_instance == null) {
            _instance = new GlobalFlags();
        }

        return _instance;
    }
}