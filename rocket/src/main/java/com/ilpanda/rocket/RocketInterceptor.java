package com.ilpanda.rocket;

import android.net.NetworkInfo;

import java.io.File;
import java.io.IOException;

public abstract class RocketInterceptor {

    public abstract boolean canInterceptor(RocketRequest request);

    public abstract File interceptor(RocketRequest request) throws IOException;

    public boolean shouldRetry(boolean airplaneMode, NetworkInfo info) {
        return false;
    }

    public boolean supportsReplay() {
        return false;
    }
}
