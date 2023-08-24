package com.hn2.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CacheUtil {

    private static CacheManager cacheManager;

    @Autowired
    public void CacheUtils(CacheManager cacheManager) {
        CacheUtil.cacheManager = cacheManager;
    }

    public static Object get(String cacheName, String key) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            return cache.get(key);
        }
        return null;
    }

    public static <T> T get(String cacheName, String key, Class<T> clazz) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            return cache.get(key, clazz);
        }
        return null;
    }

    public static void put(String cacheName, String key, Object value) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    public static boolean remove(String cacheName, String key) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            return cache.evictIfPresent(key); //去除指定key 如果有的話
        }
        return false;
    }

    public static Set<String> cacheKeys(String cacheName) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            Map<String, Object> nativeCache = (ConcurrentHashMap<String, Object>) cache.getNativeCache();
            Set<String> keySet = nativeCache.keySet();
            return keySet;
        }
        return null;
    }

    /**
     * 獲得Cache
     *
     * @return Cache
     */
    private static Cache getCache(String cacheName) {
        Cache cache = null;
        if(StringUtils.hasLength(cacheName)) {
            cache = cacheManager.getCache(cacheName);
        }
        return cache;
    }
}
