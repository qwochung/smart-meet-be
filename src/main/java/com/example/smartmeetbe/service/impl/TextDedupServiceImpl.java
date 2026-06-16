package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.service.TextDedupService;
import com.example.smartmeetbe.util.TextOverlapUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TextDedupServiceImpl implements TextDedupService {

    private Cache<String, String> lastWordsCache;

    @PostConstruct
    void init() {
        this.lastWordsCache = Caffeine.newBuilder()
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .recordStats()
                .build();
    }

    @Override
    public String deduplicate(String roomId, String participantId, String newText) {
        if (newText == null || newText.isBlank()) {
            log.warn("Received empty text from participant {} in room {}", participantId, roomId);
            return "";
        }

        String cacheKey = roomId + "::" + participantId;
        String lastWords = lastWordsCache.getIfPresent(cacheKey);
        String cleanText = TextOverlapUtil.removeOverlap(lastWords, newText);

        if (!cleanText.isBlank()) {
            lastWordsCache.put(cacheKey, TextOverlapUtil.extractContext(cleanText));
        }

        return cleanText;
    }
}
