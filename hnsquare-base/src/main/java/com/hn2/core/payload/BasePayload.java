package com.hn2.core.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;

@Data
public abstract class BasePayload implements Serializable {
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