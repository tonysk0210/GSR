package com.hn2.util;

public enum ErrorType {

    UNKNOWN(500, "未知的錯誤"),

    REQUEST_NOT_READABLE(400, "無法解析的請求"),

    REQUEST_NOT_VALID(400, "缺少必要條件"),

    INVALID_OPERATION_PERMISSION(403, "權限不足"),

    RESOURCE_EXISTED(403, "資源已存在"),

    RESOURCE_NOT_FOUND(404, "資源不存在"),

    RESOURCE_CONSTANT(406, "資源不可變更"),

    REQUEST_NOT_ALLOW(400, "輸入資料不合法");

    private final int status;
    private final String message;

    ErrorType(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
