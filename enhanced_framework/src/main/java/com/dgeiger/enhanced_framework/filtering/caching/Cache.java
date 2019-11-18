package com.dgeiger.enhanced_framework.filtering.caching;

public interface Cache {
    enum FilterResult {match, no_match, unknown}

    void storeFilterResult(String messageId, FilterResult filterResult);
    FilterResult getFilterResult(String messageId);
}
