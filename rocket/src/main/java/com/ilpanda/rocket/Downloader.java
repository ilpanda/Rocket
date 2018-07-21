package com.ilpanda.rocket;

import java.io.File;
import java.io.IOException;

public interface Downloader {

    File download(RocketRequest request) throws IOException;

}
