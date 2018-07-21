package com.ilpanda.rocket;

import java.io.File;
import java.io.IOException;

public interface RocketInterceptor {

    boolean canInterceptor(RocketRequest request);

    File interceptor(RocketRequest request) throws IOException;

}
