package com.ilpanda.rocket;

import android.os.StatFs;

import java.io.Closeable;
import java.io.File;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;


public class Utils {

    public static long getAvailableDiskSize(File file) {
        try {
            StatFs statFs = new StatFs(file.getAbsolutePath());
            //noinspection deprecation
            long blockCount =
                    SDK_INT < JELLY_BEAN_MR2 ? (long) statFs.getBlockCount() : statFs.getBlockCountLong();
            //noinspection deprecation
            long blockSize =
                    SDK_INT < JELLY_BEAN_MR2 ? (long) statFs.getBlockSize() : statFs.getBlockSizeLong();
            long available = blockCount * blockSize;
            return available;
        } catch (IllegalArgumentException ignored) {


        }
        return 0;
    }

    public static boolean createDir(File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    public static void closeQuietly(Closeable... closeables) {
        if (closeables == null) return;
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
