package com.dgeiger.enhanced_framework.filtering.filters.staticfilter;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.filtering.FilterCondition;

import java.util.List;

public abstract class StaticFilter<T> implements Filter {

    List<FilterCondition<T, StaticFilterCriterion>> filterConditions;

    protected abstract boolean matchesCondition(OFlowMessage message, FilterCondition<T, StaticFilterCriterion> condition);

    @Override
    public boolean matches(OFlowMessage message) {
        return filterConditions.stream().allMatch(condition -> this.matchesCondition(message, condition));
    }
}
