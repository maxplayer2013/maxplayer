package com.example.tinyplayer;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * 
 * @author TIAN Yu
 * 
 */
public class InlineUtil {
    public static final boolean DEBUG = true;

    /**
     * for turn off wifi / wifi-ap
     */
    public static enum CONFLICT {
        NONE, WIFI, WIFI_AP, WIFI_DIRECT,
    }

    /**
     * messages for inner communication
     */
    public static final int INNER_COMMAND_BASE = 5200;
    private static final int BASE = INNER_COMMAND_BASE;

    public static final int INNER_COMMAND_RESOLVE_CONFLICT_AND_TURN_ON_WIDI = BASE + 1;
    public static final int INNER_COMMAND_TURN_ON_WIDI = BASE + 2;
    public static final int INNER_COMMAND_SCAN = BASE + 3;
    public static final int INNER_COMMAND_DEVICE_NAME_UPDATED = BASE + 4;
    public static final int INNER_COMMAND_CLEAR_DEVICE_LIST = BASE + 5;
    public static final int INNER_COMMAND_INTRO_VIDEO_FINISHED = BASE + 6;
    public static final int INNER_COMMAND_CONNECT_FAVORITE_DEVICE = BASE + 7;
    public static final int INNER_COMMAND_CONNECT_QR_DEVICE = BASE + 8;
    public static final int INNER_COMMAND_FW_UPGRADE_ACK = BASE + 9;
    public static final int INNER_COMMAND_TEST = BASE + 99;

    // public static final int INNER_COMMAND

    /**
     * convenient log class
     * 
     * @author b103
     * 
     */
    public static class Leg {
        public static final String prefix = "B5-";

        public static void i(String tag, String content) {
            if (DEBUG) {
                Log.i(prefix + tag, content);
            }
        }

        public static void d(String tag, String content) {
            if (DEBUG) {
                Log.d(prefix + tag, content);
            }
        }

        public static void e(String tag, String content) {
            if (DEBUG) {
                Log.e(prefix + tag, content);
            }
        }

        public static void v(String tag, String content) {
            if (DEBUG) {
                Log.v(prefix + tag, content);
            }
        }

        //
        public static void i(boolean enable, String tag, String content) {
            if (DEBUG && enable) {
                Log.i(prefix + tag, content);
            }
        }

        public static void d(boolean enable, String tag, String content) {
            if (DEBUG && enable) {
                Log.d(prefix + tag, content);
            }
        }

        public static void e(boolean enable, String tag, String content) {
            if (DEBUG && enable) {
                Log.e(prefix + tag, content);
            }
        }

        public static void v(boolean enable, String tag, String content) {
            if (DEBUG && enable) {
                Log.v(prefix + tag, content);
            }
        }
    }

    public static boolean isJellyBeanOrGreater() {
        if (Build.VERSION.SDK_INT >= 16) {
            return true;
        } else {
            return false;
        }
    }

    public static int getMediaType(Uri uri) {
        String path = uri.getPath();
        if (path.contains("/video/")) {
            return 1;
        }

        if (path.contains("/images/")) {
            return 2;
        }

        return 0;
    }

}
