package com.ilpanda.rocket;

import java.io.IOException;


public class UnExpectedResponseCodeException extends IOException {

    public UnExpectedResponseCodeException(String msg) {
        super(msg);
    }
}
