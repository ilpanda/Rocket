package com.ilpanda.rocket;

import java.io.IOException;

public class PrepareException extends IOException {

    public PrepareException(String message) {
        super(message);
    }

    public PrepareException(Throwable cause) {
        super(cause);
    }

    public PrepareException(String message, Throwable cause) {
        super(message, cause);
    }
}
