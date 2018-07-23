package com.ilpanda.rocket;

import java.io.IOException;

public class TransformException extends IOException {

    public TransformException(String message) {
        super(message);
    }

    public TransformException(Throwable cause) {
        super(cause);
    }

    public TransformException(String message, Throwable cause) {
        super(message, cause);
    }

}
