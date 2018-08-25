package com.ilpanda.rocket;

import android.util.Log;


public interface Logger {

    void log(Level level, String msg);

    void log(Level level, String msg, Throwable throwable);

    public enum Level {

        VERBOSE(2),

        DEBUG(3),

        INFO(4),

        WARN(5),

        ERROR(6);

        int level;

        Level(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

    }

    public class AndroidLogger implements Logger {

        private final String tag;

        public AndroidLogger(String tag) {
            this.tag = tag;
        }

        @Override
        public void log(Level level, String msg) {
            Log.println(mapLevel(level), tag, msg);

        }

        @Override
        public void log(Level level, String msg, Throwable throwable) {
            Log.println(mapLevel(level), tag, msg + "\n" + Log.getStackTraceString(throwable));
        }

        public static int mapLevel(Level level) {
            if (level == Level.VERBOSE) { // below VERBOSE
                return Log.VERBOSE;
            } else if (level == Level.DEBUG) { // below DEBUG
                return Log.DEBUG;
            } else if (level == Level.INFO) { // below INFO
                return Log.INFO;
            } else if (level == Level.WARN) { // below WARN
                return Log.WARN;
            } else {
                return Log.ERROR;
            }
        }

    }


}
