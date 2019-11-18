package com.dgeiger.enhanced_framework.filtering.filters.staticfilter;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.FilterCondition;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaticTypeFilter extends StaticFilter<String> {

    private static final Logger log = LoggerFactory.getLogger(StaticTypeFilter.class);

    public StaticTypeFilter(String type) {
        filterConditions = new ArrayList<>();
        addType(type);
    }

    public void addType(String type){
        filterConditions.add(new FilterCondition<>(StaticFilterCriterion.TYPE, type));
    }

    @Override
    protected boolean matchesCondition(OFlowMessage message, FilterCondition<String, StaticFilterCriterion> condition) {
        if(!message.getType().name().equalsIgnoreCase(condition.getValue())){
            //log.debug("message {} does not match type filter ({})", message.getXid(), message.getType().name());
            return false;
        }

        return true;
    }

    @Override
    public List<OFlowMessageField> getRelevantMessageFields() {
        return Collections.singletonList(OFlowMessageField.TYPE);
    }
}
