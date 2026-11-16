package com.zoopass.common.exception;

/** 404 - the requested entity does not exist (or is soft-deleted). */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy " + resource + " với mã " + id);
    }
}
