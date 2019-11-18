package com.dgeiger.enhanced_framework.filtering.filters.staticfilter;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.FilterCondition;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaticDirectionFilter extends StaticFilter<Boolean> {

    private static final Logger log = LoggerFactory.getLogger(StaticDirectionFilter.class);

    public StaticDirectionFilter(boolean upstream) {
        filterConditions = new ArrayList<>();
        setDirection(upstream);
    }

    public void setDirection(boolean upstream) {
        filterConditions.clear();
        filterConditions.add(new FilterCondition<>(StaticFilterCriterion.DIRECTION, upstream));
    }

    @Override
    protected boolean matchesCondition(OFlowMessage message, FilterCondition<Boolean, StaticFilterCriterion> condition) {
        if(message.isUpStreamDirection() != condition.getValue()){
            //log.debug("message {} does not match direction filter ({})", message.getXid(), message.getSender());
            return false;
        }
        return true;
    }

    @Override
    public List<OFlowMessageField> getRelevantMessageFields() {
        return Collections.singletonList(OFlowMessageField.DIRECTION);
    }
}
