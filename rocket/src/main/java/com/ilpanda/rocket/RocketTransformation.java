package com.ilpanda.rocket;

import java.io.File;

public interface RocketTransformation {

    File transform(RocketRequest request, File file) throws TransformException;

}
