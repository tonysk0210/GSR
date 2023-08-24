package com.hn2.util;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class RespHelper {
    public static Map<String, Object> ok() {
        return RespHelper.ok(null);
    }

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", "success");
        map.put("data", data);
        map.put("time", LocalDateTime.now());

        return map;
    }

    public static Map<String, Object> error(int code, String message, Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        map.put("data", data);
        map.put("time", LocalDateTime.now());
        return map;
    }

    public static Map<String, Object> error(String message, Object data) {
        return RespHelper.error(500, message, data);
    }
}
