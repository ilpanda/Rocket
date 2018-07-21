package com.ilpanda.rocket;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

public class UrlInterceptor implements RocketInterceptor {
    @Override
    public boolean canInterceptor(RocketRequest request) {
        String url = request.getUrl();
        return TextUtils.isEmpty(url);
    }

    @Override
    public File interceptor(RocketRequest request) throws IOException {
        throw new ResourceInvalidException("url  cannot  be  null");
    }
}
