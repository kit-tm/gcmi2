package com.dgeiger.enhanced_framework.filtering.filters.staticfilter;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.FilterCondition;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaticPercentageFilter extends StaticFilter<Integer> {

    private int matchingPercentage;
    private ArrayList<Boolean> nextFilterResults;

    public StaticPercentageFilter(int matchingPercentage) {
        filterConditions = new ArrayList<>();
        this.matchingPercentage = matchingPercentage;
        nextFilterResults = new ArrayList<>();
        filterConditions.add(new FilterCondition<>(StaticFilterCriterion.SENDER, 1));
    }

    private void generateNextFilterResults(){
        for(int i = 0; i < 10; i++){
            if(i < matchingPercentage / 10){
                nextFilterResults.add(true);
            }else{
                nextFilterResults.add(false);
            }
        }
        Collections.shuffle(nextFilterResults);
    }

    @Override
    protected boolean matchesCondition(OFlowMessage message, FilterCondition<Integer, StaticFilterCriterion> condition) {
        if(nextFilterResults.size() == 0) generateNextFilterResults();
        return nextFilterResults.remove(0);
    }

    @Override
    public List<OFlowMessageField> getRelevantMessageFields() {
        return new ArrayList<>();
    }
}
