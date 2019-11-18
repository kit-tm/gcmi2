package com.dgeiger.enhanced_framework.filtering.filters.timebasedfilter;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A time-based filter only applies a filter when in active mode
 * It switches to active mode if a defined number of messages received
 * in a defined time period matches an activation filter
 */
public class TimeBasedFilter implements Filter {

    /**
     * Filter that matches activation messages
     */
    private Filter activationFilter;

    /**
     * Maximum time in ms waited to receive necessary number of activation messages
     */
    private long maxTimeIntervalLength;

    /**
     * Number of activation messages required
     */
    private int minNumberOfActivationMessages;

    private int numberOfActivationMessages;

    private Timer timer;


    public TimeBasedFilter(Filter activationFilter, long maxTimeIntervalLength, int minNumberOfActivationMessages) {
        this.activationFilter = activationFilter;
        this.maxTimeIntervalLength = maxTimeIntervalLength;
        this.minNumberOfActivationMessages = minNumberOfActivationMessages;
        numberOfActivationMessages = 0;
        timer = new Timer();
    }

    public Filter getActivationFilter() {
        return activationFilter;
    }

    public void setActivationFilter(Filter activationFilter) {
        this.activationFilter = activationFilter;
    }

    public long getMaxTimeIntervalLength() {
        return maxTimeIntervalLength;
    }

    public void setMaxTimeIntervalLength(long maxTimeIntervalLength) {
        this.maxTimeIntervalLength = maxTimeIntervalLength;
    }

    public int getMinNumberOfActivationMessages() {
        return minNumberOfActivationMessages;
    }

    public void setMinNumberOfActivationMessages(int minNumberOfActivationMessages) {
        this.minNumberOfActivationMessages = minNumberOfActivationMessages;
    }

    @Override
    public boolean matches(OFlowMessage message) {
        // count number of activation messages
        if(activationFilter.matches(message)){
            numberOfActivationMessages++;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    numberOfActivationMessages--;
                }
            }, maxTimeIntervalLength);
        }

        return numberOfActivationMessages >= minNumberOfActivationMessages;
    }

    @Override
    public List<OFlowMessageField> getRelevantMessageFields() {
        return activationFilter.getRelevantMessageFields();
    }
}
