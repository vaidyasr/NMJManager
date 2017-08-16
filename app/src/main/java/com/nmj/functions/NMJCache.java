package com.nmj.functions;

import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class NMJCache {
    private static LoadingCache<String, String> tmdbCache;

    static {
        tmdbCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(
                        new CacheLoader<String, String>() {
                            @Override
                            public String load(String id) throws Exception {
                                tmdbCache.put(id, "");
                                return tmdbCache.getIfPresent(id);
                            }
                        }
                );
    }

    public static LoadingCache<String, String> getLoadingCache() {
        return tmdbCache;
    }
}