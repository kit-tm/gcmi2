package com.dgeiger.enhanced_framework;

import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.filtering.FilterApp;
import com.dgeiger.enhanced_framework.filtering.FilterLayer;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;
import com.dgeiger.enhanced_framework.filtering.caching.Cache;
import com.dgeiger.enhanced_framework.filtering.caching.HashMapCache;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.reflect.Whitebox.getInternalState;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { FilterLayer.class })
public class FilterLayerTest {

    private FilterLayer filterLayer;
    private FilterApp filterApp;
    private Context context;

    private void createFilterLayer(int numberOfMatchingFilters, int numberOfNonMatchingFilters, boolean matchModeAny, boolean useCache){
        ArrayList<Filter> filters = new ArrayList<>();

        for(int i = 0; i < numberOfMatchingFilters; i++){
            Filter matchingFilter = mock(Filter.class);
            when(matchingFilter.matches(any())).thenReturn(true);
            filters.add(matchingFilter);
        }

        for(int i = 0; i < numberOfNonMatchingFilters; i++){
            Filter matchingFilter = mock(Filter.class);
            when(matchingFilter.matches(any())).thenReturn(false);
            filters.add(matchingFilter);
        }

        filterApp = mock(FilterApp.class);
        when(filterApp.getFilters()).thenReturn(filters);

        if(filters.size() == 2){
            when(filters.get(0).getRelevantMessageFields()).thenReturn(Collections.singletonList(OFlowMessageField.XID));
            when(filters.get(1).getRelevantMessageFields()).thenReturn(Collections.singletonList(OFlowMessageField.SIZE));
        }

        filterLayer = new FilterLayer(filterApp, matchModeAny, useCache);
        context = mock(Context.class);
        when(context.controllerConnection()).thenReturn(message -> {});
        when(context.switchConnection()).thenReturn(message -> {});
    }

    @Test
    public void testForwardMessageWhenFilterMatches(){
        createFilterLayer(1, 0, false, false);
        OFlowMessage oFlowMessage = mock(OFlowMessage.class);

        filterLayer.receivedFromController(oFlowMessage, context);
        verify(filterApp, times(1)).receivedFromController(oFlowMessage, context);

        filterLayer.receivedFromSwitch(oFlowMessage, context);
        verify(filterApp, times(1)).receivedFromSwitch(oFlowMessage, context);
    }

    @Test
    public void testDoNotForwardMessageWhenFilterNotMatches(){
        createFilterLayer(0, 1, false, false);
        OFlowMessage oFlowMessage = mock(OFlowMessage.class);

        filterLayer.receivedFromController(oFlowMessage, context);
        verify(filterApp, times(0)).receivedFromController(oFlowMessage, context);

        filterLayer.receivedFromSwitch(oFlowMessage, context);
        verify(filterApp, times(0)).receivedFromSwitch(oFlowMessage, context);
    }

    @Test
    public void testForwardWhenAtLeastOneMatches(){
        createFilterLayer(1, 1, true, false);
        OFlowMessage oFlowMessage = mock(OFlowMessage.class);

        filterLayer.receivedFromController(oFlowMessage, context);
        verify(filterApp, times(1)).receivedFromController(oFlowMessage, context);

        filterLayer.receivedFromSwitch(oFlowMessage, context);
        verify(filterApp, times(1)).receivedFromSwitch(oFlowMessage, context);
    }

    @Test
    public void testForwardWhenAtLeastOneNotMatches(){
        createFilterLayer(0, 1, true, false);
        OFlowMessage oFlowMessage = mock(OFlowMessage.class);

        filterLayer.receivedFromController(oFlowMessage, context);
        verify(filterApp, times(0)).receivedFromController(oFlowMessage, context);

        filterLayer.receivedFromSwitch(oFlowMessage, context);
        verify(filterApp, times(0)).receivedFromSwitch(oFlowMessage, context);
    }

    @Test
    public void testForwardWhenNotAllMatch(){
        createFilterLayer(1, 1, false, false);
        OFlowMessage oFlowMessage = mock(OFlowMessage.class);

        filterLayer.receivedFromController(oFlowMessage, context);
        verify(filterApp, times(0)).receivedFromController(oFlowMessage, context);

        filterLayer.receivedFromSwitch(oFlowMessage, context);
        verify(filterApp, times(0)).receivedFromSwitch(oFlowMessage, context);
    }

    @Test
    public void testCacheForMatchingFilters(){
        createFilterLayer(2, 0, false, true);

        HashMapCache cache = getInternalState(filterLayer, "upstreamCache");
        assertThat(cache, is(not(nullValue())));

        OFlowMessage oFlowMessage = mock(OFlowMessage.class);
        when(oFlowMessage.getSize()).thenReturn(100);
        when(oFlowMessage.getXid()).thenReturn(123L);

        filterLayer.receivedFromSwitch(oFlowMessage, context);
        filterLayer.receivedFromSwitch(oFlowMessage, context);

        assertThat(cache.getFilterResult("100||||123|"), is(Cache.FilterResult.match));
        verify(filterApp, times(2)).receivedFromSwitch(oFlowMessage, context);
    }

    @Test
    public void testCacheForNonMatchingFilters(){
        createFilterLayer(0, 2, false, true);

        HashMapCache cache = getInternalState(filterLayer, "upstreamCache");
        assertThat(cache, is(not(nullValue())));

        OFlowMessage oFlowMessage = mock(OFlowMessage.class);
        when(oFlowMessage.getSize()).thenReturn(100);
        when(oFlowMessage.getXid()).thenReturn(123L);

        filterLayer.receivedFromSwitch(oFlowMessage, context);
        filterLayer.receivedFromSwitch(oFlowMessage, context);

        assertThat(cache.getFilterResult("100||||123|"), is(Cache.FilterResult.no_match));
        verify(filterApp, times(0)).receivedFromSwitch(oFlowMessage, context);
    }
}
