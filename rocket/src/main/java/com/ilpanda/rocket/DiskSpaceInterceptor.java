package com.ilpanda.rocket;

import java.io.File;
import java.io.IOException;

public class DiskSpaceInterceptor extends RocketInterceptor {

    private long availableDiskSize;

    private long needDiskSize;

    @Override
    public boolean canInterceptor(RocketRequest request) {

        if (request.getFileSize() == 0) {
            return false;
        }

        File targetFile = request.getTemFile();

        float scale = 1.3f;

        long needDiskSize = (long) (request.getFileSize() * scale);

        long availableDiskSize = Utils.getAvailableDiskSize(targetFile.getParentFile());

        if (availableDiskSize < needDiskSize) {
            this.availableDiskSize = availableDiskSize;
            this.needDiskSize = needDiskSize;
            return true;
        }

        return false;
    }

    @Override
    public File interceptor(RocketRequest request) throws IOException {
        throw new SpaceUnAvailableException("disk space is unavailable . the availableDiskSize is : " +
                " " + availableDiskSize + " --  the request need size:" + needDiskSize);
    }
}
