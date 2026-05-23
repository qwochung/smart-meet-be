package com.example.smartmeetbe.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TextDedupService {

    private Cache<String, String> lastWordsCache;

    @PostConstruct
    void init() {
        this.lastWordsCache = Caffeine.newBuilder()
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .recordStats()
                .build();
    }

    public String deduplicate(String roomId, String participantId, String newText) {
        if (newText == null || newText.isBlank()) {
            log.warn("Received empty text from participant {} in room {}", participantId, roomId);
            return "";
        }

        String cacheKey = roomId + "::" + participantId;
        String lastWords = lastWordsCache.getIfPresent(cacheKey);
        String cleanText;

        if (lastWords == null || lastWords.isBlank()) {
            cleanText = newText.trim();
        } else {
            cleanText = removeOverlap(lastWords, newText.trim());
        }

        if (!cleanText.isBlank()) {
            lastWordsCache.put(cacheKey, extractLastNWords(cleanText, 10));
        }

        return cleanText;
    }

    private String removeOverlap(String oldSuffix, String newText) {
        String[] oldWords = oldSuffix.split("\\s+");
        String[] newWords = newText.split("\\s+");

        int overlapCount = 0;
        for (int i = 0; i < Math.min(oldWords.length, newWords.length); i++) {
            boolean match = true;
            for (int j = 0; j <= i; j++) {
                if (!oldWords[oldWords.length - 1 - i + j].equals(newWords[j])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                overlapCount = i + 1;
            }
        }

        if (overlapCount > 0) {
            String[] remaining = Arrays.copyOfRange(newWords, overlapCount, newWords.length);
            log.info("Trimmed {} overlapping word(s) from transcript", overlapCount);
            return String.join(" ", remaining);
        }
        return newText;
    }

    private String extractLastNWords(String text, int n) {
        String[] words = text.trim().split("\\s+");
        if (words.length <= n) {
            return text.trim();
        }
        return Arrays.stream(words, words.length - n, words.length)
                .collect(Collectors.joining(" "));
    }
}
