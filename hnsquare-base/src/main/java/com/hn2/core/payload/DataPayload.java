package com.hn2.core.payload;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataPayload extends BasePayload {
    /** 單筆物件 */
    private final Object data;
}