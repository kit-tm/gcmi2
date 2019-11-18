package com.dgeiger.enhanced_framework.filtering.filters.staticfilter;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.filtering.FilterCondition;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.Masked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StaticMatchFilter extends StaticFilter<Masked<IPv4Address>> {

    private static final Logger log = LoggerFactory.getLogger(StaticMatchFilter.class);

    public StaticMatchFilter() {
        filterConditions = new ArrayList<>();
    }

    public void setSourceNetmask(Masked<IPv4Address> netmask){
        filterConditions = filterConditions
                .stream()
                .filter(condition -> condition.getFilterCriterion() != StaticFilterCriterion.NETMASK_SRC)
                .collect(Collectors.toList());

        filterConditions.add(new FilterCondition<>(StaticFilterCriterion.NETMASK_SRC, netmask));
    }

    public void setDestNetmask(Masked<IPv4Address> netmask){
        filterConditions = filterConditions
                .stream()
                .filter(condition -> condition.getFilterCriterion() != StaticFilterCriterion.NETMASK_DST)
                .collect(Collectors.toList());

        filterConditions.add(new FilterCondition<>(StaticFilterCriterion.NETMASK_DST, netmask));
    }

    @Override
    protected boolean matchesCondition(OFlowMessage message, FilterCondition<Masked<IPv4Address>, StaticFilterCriterion> condition) {
        Match match = message.getMatch();
        if(match == null) return false;

        MatchField matchField;

        if(condition.getFilterCriterion() == StaticFilterCriterion.NETMASK_SRC){
            matchField = MatchField.IPV4_SRC;
        }else if(condition.getFilterCriterion() == StaticFilterCriterion.NETMASK_DST){
            matchField = MatchField.IPV4_DST;
        }else{
            return false;
        }

        // message matches filter if the set of packets matched by its match field
        // is equal or a subset of the set of packets allowed by the filters match field

        // filters mask is fully wildcarded and matches all addresses
        if(condition.getValue().getMask().equals(IPv4Address.of(0))) return true;

        IPv4Address srcAddress = (IPv4Address) match.get(matchField);

        // Match from OpenFlow message does not contain IPv4 src address
        if(srcAddress == null) return false;

        // can be null if ip address in message match field does not contain wild bits
        Masked<IPv4Address> srcAddressMasked = match.getMasked(matchField);

        int wildBitsOfMessageMatchField = 0;
        if(srcAddressMasked != null){
            wildBitsOfMessageMatchField = 32 - srcAddressMasked.getMask().asCidrMaskLength();
        }
        int wildBitsOfFilter = 32 - condition.getValue().getMask().asCidrMaskLength();

        IPv4Address maskedAddressFromMessageMatchField = (IPv4Address) match.get(matchField);

        if (wildBitsOfFilter < wildBitsOfMessageMatchField){
            return false;
        }else if(wildBitsOfMessageMatchField < wildBitsOfFilter){
            maskedAddressFromMessageMatchField = maskedAddressFromMessageMatchField.applyMask(IPv4Address.ofCidrMaskLength(32 - wildBitsOfFilter));
        }else if(srcAddressMasked != null){
            maskedAddressFromMessageMatchField = srcAddressMasked.getValue().and(condition.getValue().getMask());
        }

        // compare relevant parts (as specified by mask) of both ip addresses
        return maskedAddressFromMessageMatchField.equals(condition.getValue().getValue().and(condition.getValue().getMask()));
    }

    @Override
    public List<OFlowMessageField> getRelevantMessageFields() {
        return Collections.singletonList(OFlowMessageField.MATCH_FIELD);
    }
}
