package com.example.smartmeetbe.strategy;

import com.example.smartmeetbe.constant.MeetingType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MeetingSummaryContext {

    private final Map<MeetingType, MeetingSummaryStrategy> strategyMap;

    public MeetingSummaryContext(List<MeetingSummaryStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(MeetingSummaryStrategy::getTypeCode, Function.identity()));
    }

    public MeetingSummaryStrategy getStrategy(MeetingType typeCode) {
        MeetingSummaryStrategy strategy = strategyMap.get(typeCode);
        if (strategy == null) {
            return strategyMap.get(MeetingType.GENERAL); // Fallback mặc định
        }
        return strategy;
    }
}
