package com.ilpanda.rocket;

import java.io.File;
import java.io.IOException;

public class NetworkInterceptor implements RocketInterceptor {


    private RocketDownloader downloader;

    public  NetworkInterceptor(RocketDownloader downloader) {
        this.downloader = downloader;
    }

    @Override
    public boolean canInterceptor(RocketRequest data) {
        return true;
    }

    @Override
    public File interceptor(RocketRequest request) throws IOException {
        File result = this.downloader.download(request);
        if (result == null || !result.exists()) {
            throw new RenameFileException("rename file  failed" + request);
        }
        return result;
    }

}
