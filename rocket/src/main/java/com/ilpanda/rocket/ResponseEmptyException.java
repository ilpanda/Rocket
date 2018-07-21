package com.ilpanda.rocket;

import java.io.IOException;


public class ResponseEmptyException extends IOException {

    public ResponseEmptyException(String msg) {
        super(msg);
    }

}
