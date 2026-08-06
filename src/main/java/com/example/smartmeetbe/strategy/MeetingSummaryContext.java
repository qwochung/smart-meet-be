package com.example.smartmeetbe.strategy;

import com.example.smartmeetbe.constant.MeetingType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MeetingSummaryContext {

    private final Map<String, MeetingSummaryStrategy> strategyMap;

    public MeetingSummaryContext(List<MeetingSummaryStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(MeetingSummaryStrategy::getTypeCode, Function.identity()));
    }

    /**
     * @param typeCode mã loại cuộc họp của phòng; loại dựng sẵn có strategy riêng, loại do
     *                 người dùng tự tạo dùng chung {@code CustomTypeSummaryStrategy}.
     */
    public MeetingSummaryStrategy getStrategy(String typeCode) {
        MeetingSummaryStrategy strategy = strategyMap.get(typeCode);
        if (strategy != null) {
            return strategy;
        }
        if (typeCode != null && !MeetingType.isBuiltIn(typeCode)) {
            MeetingSummaryStrategy custom = strategyMap.get(MeetingSummaryStrategy.CUSTOM_TYPE_CODE);
            if (custom != null) {
                return custom;
            }
        }
        return strategyMap.get(MeetingType.GENERAL.name()); // Fallback mặc định
    }
}
