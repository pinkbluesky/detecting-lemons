package frc.robot;

import java.util.HashMap;
import java.util.Map;

/**
 * Store and retrieve global robot states.
 */
public class BigData {
    private static Map<String, String> map;

    public static void start() {
        map = new HashMap<String, String>();
    }

    /** put (or update) a key/value mapping into the map */
    public static void put(String key, String val) {
        map.put(key, val);
    }

    /** put (or update) a key/value mapping into the map */
    public static void put(String key, double val) {
        map.put(key, "" + val);
    }

    /** put (or update) a key/value mapping into the map */
    public static void put(String key, int val) {
        map.put(key, "" + val);
    }

    /** put (or update) a key/value mapping into the map */
    public static void put(String key, boolean val) {
        map.put(key, "" + val);
    }

    private static void existenceCheck(String key, String type) {
        if (!map.containsKey(key)) {
            switch (type) {
            case "boolean":
                put(key, false);
                break;
            case "double":
                put(key, 0.0);
                break;
            case "int":
                put(key, 0);
                break;
            case "String":
                put(key, "");
                break;
            }
        }
    }

    /**
     * Get the int config value corresponding to the key passed in.
     * 
     * @return The corresponding integer value, or -1 if the key was not
     *         found/invalid
     */
    public static int getInt(String key) {
        existenceCheck(key, "int");
        try {
            return Integer.parseInt(map.get(key));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Get the string value corresponding to the key passed in.
     * 
     * @return The corresponding string value, or the empty string if the key was
     *         not found/invalid
     */
    public static String getString(String key) {
        existenceCheck(key, "String");
        return map.get(key);
    }

    /**
     * Get the boolean config value corresponding to the key passed in.
     * 
     * @return The corresponding boolean value, or false if the key was invalid
     */
    public static boolean getBoolean(String key) {
        existenceCheck(key, "boolean");
        return Boolean.parseBoolean(map.get(key));
    }

    public static void putJetsonCameraConnected(boolean connected) {
        put("jetson_camera_connected", connected);
    }

    public static boolean getJetsonCameraConnected() {
        return getBoolean("jetson_camera_connected");
    }

    /**
     * @param r the range (distance from the target horizontally, in inches)
     * @param a the azimuth (in degrees, where positive means camera is pointed to
     *          the left)
     * @param x TODO TELL ME WHAT THIS IS!
     * @param y TODO TELL ME WHAT THIS IS!!!!!!!!!!!!!!
     */
    public static void updateCamera(double r, double a, double x, double y) {
        put("camera_azimuth", a);
        put("camera_range", r);
        put("relative_x", x);
        put("relative_y", y);
    }

}
