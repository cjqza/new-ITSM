package com.cenziang.itsm.domain;

public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static BusinessException validation(String message) {
        return new BusinessException("VALIDATION_ERROR", message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException("FORBIDDEN", message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException("NOT_FOUND", message);
    }

    public static BusinessException illegalStatus(String message) {
        return new BusinessException("ILLEGAL_TICKET_STATUS", message);
    }
}
