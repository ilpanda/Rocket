package com.ilpanda.rocket;

import android.text.TextUtils;

import java.io.File;

public class ChecksumTransformation implements RocketTransformation {

    @Override
    public File transform(RocketRequest request, File file) throws TransformException {

        String fileMd5 = request.getFileMd5();

        if (TextUtils.isEmpty(fileMd5)) {
            throw new ChecksumException("the  request  file  md5  is empty");
        }

        String md5 = Utils.calculateMD5(file);
        if (!fileMd5.equalsIgnoreCase(md5)) {
            throw new ChecksumException("request file md5 : " + fileMd5 + "---- the download file md5 : " + md5);
        }

        return file;
    }
}
