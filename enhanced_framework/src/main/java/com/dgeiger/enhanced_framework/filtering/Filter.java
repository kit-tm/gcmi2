package com.dgeiger.enhanced_framework.filtering;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;

import java.util.List;

public interface Filter {

    boolean matches(OFlowMessage message);
    List<OFlowMessageField> getRelevantMessageFields();

}
