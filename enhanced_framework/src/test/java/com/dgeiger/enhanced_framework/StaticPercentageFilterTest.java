package com.dgeiger.enhanced_framework;

import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticPercentageFilter;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class StaticPercentageFilterTest {

    @Test
    public void filterMatchesGivenPercentageOfMessages(){

        for(int x = 0; x <= 10; x++){
            int percentageToTest = x * 10;

            StaticPercentageFilter staticPercentageFilter = new StaticPercentageFilter(percentageToTest);
            int matchCounter = 0;
            OFlowMessage oFlowMessage = mock(OFlowMessage.class);
            for(int i = 0; i < 1000000; i++){
                if(staticPercentageFilter.matches(oFlowMessage)) matchCounter++;
            }

            assertThat(matchCounter, comparesEqualTo(percentageToTest * 10000));
        }

    }
}
