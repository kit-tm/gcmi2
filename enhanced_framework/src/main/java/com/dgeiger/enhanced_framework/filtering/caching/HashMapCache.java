package com.dgeiger.enhanced_framework.filtering.caching;

import java.util.concurrent.ConcurrentHashMap;

public class HashMapCache implements Cache {

    private ConcurrentHashMap<String, FilterResult> cache;

    public HashMapCache() {
        cache = new ConcurrentHashMap<>();
    }

    @Override
    public void storeFilterResult(String messageId, FilterResult filterResult) {
        cache.put(messageId, filterResult);
    }

    @Override
    public FilterResult getFilterResult(String messageId) {
        return cache.getOrDefault(messageId, FilterResult.unknown);
    }
}
