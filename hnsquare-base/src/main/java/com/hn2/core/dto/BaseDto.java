package com.hn2.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public abstract class BaseDto {
    /** 時戳 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    private java.time.LocalDateTime timestamp;
    /** 提示訊息 */
    private String message;
    /** 錯誤訊息 */
    private String error;
    /** 令牌 */
    private String token;

    public java.time.LocalDateTime getTimestamp() {
        return java.time.LocalDateTime.now();
    }

    /**
     * 避免序列化機密資料
     * @param stream ObjectOutputStream
     */
    private void writeObject(java.io.ObjectOutputStream stream) throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Invalid Object");
    }

}