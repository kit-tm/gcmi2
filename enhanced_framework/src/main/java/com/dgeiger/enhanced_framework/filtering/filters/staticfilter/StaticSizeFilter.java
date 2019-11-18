package com.dgeiger.enhanced_framework.filtering.filters.staticfilter;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.FilterCondition;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaticSizeFilter extends StaticFilter<Integer> {

    private static final Logger log = LoggerFactory.getLogger(StaticSizeFilter.class);

    public StaticSizeFilter() {
        filterConditions = new ArrayList<>();
    }

    public StaticSizeFilter(int minSize, int maxSize) {
        filterConditions = new ArrayList<>();
        setMinSize(minSize);
        setMaxSize(maxSize);
    }

    public void setMinSize(int size){
        filterConditions.add(new FilterCondition<>(StaticFilterCriterion.MIN_SIZE, size));
    }

    public void setMaxSize(int size){
        filterConditions.add(new FilterCondition<>(StaticFilterCriterion.MAX_SIZE, size));
    }

    @Override
    protected boolean matchesCondition(OFlowMessage message, FilterCondition<Integer, StaticFilterCriterion> condition) {
        switch(condition.getFilterCriterion()){
            case MIN_SIZE:
                if(message.getSize() < condition.getValue()){
                    //log.debug("message {} does not match min_size filter ({})", message.getXid(), message.getSize());
                    return false;
                }
                break;
            case MAX_SIZE:
                if(message.getSize() > condition.getValue()){
                    //log.debug("message {} does not match max_size filter ({})", message.getXid(), message.getSize());
                    return false;
                }
                break;
        }

        return true;
    }

    @Override
    public List<OFlowMessageField> getRelevantMessageFields() {
        return Collections.singletonList(OFlowMessageField.SIZE);
    }
}
