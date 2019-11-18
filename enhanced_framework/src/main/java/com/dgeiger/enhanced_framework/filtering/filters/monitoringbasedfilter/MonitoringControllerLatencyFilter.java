package com.dgeiger.enhanced_framework.filtering.filters.monitoringbasedfilter;

import com.dgeiger.enhanced_framework.filtering.FilterCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitoringControllerLatencyFilter extends MonitoringBasedFilter {

    private static final Logger log = LoggerFactory.getLogger(MonitoringControllerLatencyFilter.class);

    public MonitoringControllerLatencyFilter(double minValue, double maxValue){
        filterConditions.add(new FilterCondition<>(MonitoringFilterCriterion.MIN_CONTROLLER_LATENCY, minValue));
        filterConditions.add(new FilterCondition<>(MonitoringFilterCriterion.MAX_CONTROLLER_LATENCY, maxValue));
    }

    @Override
    protected boolean matchesCondition(FilterCondition<Double, MonitoringFilterCriterion> condition) {
        double currentLatency = getAverageControllerLatency();

        if(currentLatency == 0.0) return false;

        if(condition.getFilterCriterion() == MonitoringFilterCriterion.MAX_CONTROLLER_LATENCY){
            //log.debug("Comparing current latency {} to max latency {}", currentLatency, condition.getValue());
            return currentLatency <= condition.getValue();
        }

        if(condition.getFilterCriterion() == MonitoringFilterCriterion.MIN_CONTROLLER_LATENCY){
            //log.debug("Comparing current latency {} to min latency {}", currentLatency, condition.getValue());
            return currentLatency >= condition.getValue();
        }

        return false;
    }

}
