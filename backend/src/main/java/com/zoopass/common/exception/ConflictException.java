package com.zoopass.common.exception;

/** 409 - the request conflicts with current data, e.g. an email that is already registered. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
