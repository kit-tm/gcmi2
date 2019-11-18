package com.dgeiger.enhanced_framework.filtering.filters.monitoringbasedfilter;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.filtering.FilterCondition;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public abstract class MonitoringBasedFilter implements Filter {

    ArrayList<FilterCondition<Double, MonitoringFilterCriterion>> filterConditions;
    private Map<Long, Long> messagesSentToController;
    private Map<Long, Long> latencyMeasurements;

    public MonitoringBasedFilter() {
        filterConditions = new ArrayList<>();
        messagesSentToController = new HashMap<>();
        latencyMeasurements = new HashMap<>();
    }

    protected abstract boolean matchesCondition(FilterCondition<Double, MonitoringFilterCriterion> condition);

    protected double getAverageControllerLatency(){
        OptionalDouble value = latencyMeasurements.values().stream().mapToLong(aLong -> aLong).average();
        if(value.isPresent()){
            return value.getAsDouble();
        }
        return 0;
    }

    @Override
    public boolean matches(OFlowMessage message) {
        if(message.isUpStreamDirection()){
            messagesSentToController.put(message.getXid(), getCurrentTimeMicros());
        }else if(messagesSentToController.containsKey(message.getXid())){
            latencyMeasurements.put(System.currentTimeMillis(), getCurrentTimeMicros() - messagesSentToController.get(message.getXid()));
            messagesSentToController.remove(message.getXid());
        }

        latencyMeasurements = latencyMeasurements.entrySet().stream()
                .filter(map -> map.getKey() >= System.currentTimeMillis() - 1000 * 3)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return filterConditions.stream().allMatch(this::matchesCondition);
    }

    private long getCurrentTimeMicros(){
        Clock clock = Clock.systemDefaultZone();
        Instant instant = clock.instant();   // or Instant.now();
        long seconds = instant.getEpochSecond();
        long nano = instant.getNano();
        final long nanoseconds = seconds * 1000000000 + nano;
        return nanoseconds / 1000;
    }

    @Override
    public List<OFlowMessageField> getRelevantMessageFields() {
        return new ArrayList<>();
    }
}
