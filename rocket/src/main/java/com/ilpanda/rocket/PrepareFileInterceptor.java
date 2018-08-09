package com.ilpanda.rocket;

import java.io.File;
import java.io.IOException;

public class PrepareFileInterceptor extends RocketInterceptor {


    private static final String TAG = "PrepareFileInterceptor";

    private IOException exception;

    @Override
    public boolean canInterceptor(RocketRequest request) {

        String fileName = request.getFileName();
        File targetFile = request.getTargetFile();
        File targetDir;
        File temFile;

        try {

            if (targetFile == null) {
                targetDir = new File(request.rocket.getDefaultPath());
                targetFile = new File(targetDir, fileName);
                request.setTargetFile(targetFile);
            } else {
                targetDir = targetFile.getParentFile();
                fileName = targetFile.getName();
            }

            if (!targetDir.exists()) {
                boolean createDir = Utils.createDir(targetDir);
                if (!createDir) {
                    return true;
                }
            }

            temFile = new File(targetDir, fileName + ".tmp");


            if (request.forceDownload) {
                if (targetFile.exists()) {
                    targetFile.delete();
                }
            } else if (targetFile.exists()) {
                return true;
            }

            if (temFile.exists()) {
                temFile.delete();
            }

            temFile.createNewFile();

            request.setTemFile(temFile);

        } catch (IOException e) {
            this.exception = e;
            return true;
        }

        return false;
    }

    @Override
    public File interceptor(RocketRequest request) throws IOException {
        if (exception != null) {
            String error = Utils.getThreadStack(exception);
            exception = null;
            throw new PrepareFileException(error);
        }
        return request.getTargetFile();
    }
}
