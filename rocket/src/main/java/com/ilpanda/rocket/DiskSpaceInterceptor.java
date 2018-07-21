package com.ilpanda.rocket;

import java.io.File;
import java.io.IOException;

public class DiskSpaceInterceptor implements RocketInterceptor {

    @Override
    public boolean canInterceptor(RocketRequest request) {

        if (request.getFileSize() == 0) {
            return false;
        }

        File targetFile = request.getTemFile();

        float scale = 1.3f;

        long minSpace = (long) (request.getFileSize() * scale);

        if (Utils.getAvailableDiskSize(targetFile) < minSpace) {
            return true;
        }

        return false;
    }

    @Override
    public File interceptor(RocketRequest request) throws IOException {
        throw new SpaceUnAvailableException("disk space is unavailable " + request);
    }
}
