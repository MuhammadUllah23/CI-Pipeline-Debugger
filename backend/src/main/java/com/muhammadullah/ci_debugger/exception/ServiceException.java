package com.muhammadullah.ci_debugger.exception;

import java.util.HashMap;
import java.util.Map;

public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details = new HashMap<>();

    private ServiceException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public static ServiceException of(ErrorCode errorCode) {
        return new ServiceException(errorCode, null);
    }

    public static ServiceException of(ErrorCode errorCode, String customMessage) {
        return new ServiceException(errorCode, customMessage);
    }

    public ServiceException addDetail(String key, Object value) {
        details.put(key, value);
        return this;
    }

    public ErrorCode getErrorCode()       { return errorCode; }
    public Map<String, Object> getDetails() { return details; }
}
