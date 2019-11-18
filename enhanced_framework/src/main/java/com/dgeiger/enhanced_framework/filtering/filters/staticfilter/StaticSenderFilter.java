package com.dgeiger.enhanced_framework.filtering.filters.staticfilter;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.FilterCondition;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaticSenderFilter extends StaticFilter<String> {

    private static final Logger log = LoggerFactory.getLogger(StaticSenderFilter.class);

    public StaticSenderFilter(String sender) {
        filterConditions = new ArrayList<>();
        addSender(sender);
    }

    public void addSender(String value) {
        filterConditions.add(new FilterCondition<>(StaticFilterCriterion.SENDER, value));
    }

    @Override
    protected boolean matchesCondition(OFlowMessage message, FilterCondition<String, StaticFilterCriterion> condition) {
        if(!message.getSender().contains(condition.getValue())){
            //log.debug("message {} does not match sender filter ({})", message.getXid(), message.getSender());
            return false;
        }

        return true;
    }

    @Override
    public List<OFlowMessageField> getRelevantMessageFields() {
        return Collections.singletonList(OFlowMessageField.SENDER);
    }
}
