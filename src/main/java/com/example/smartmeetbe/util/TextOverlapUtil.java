package com.example.smartmeetbe.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TextOverlapUtil {

    public static final int CONTEXT_WORDS = 10;
    public static final int MIN_OVERLAP_WORDS = 2;
    public static final int MAX_OVERLAP_WORDS = 12;

    private TextOverlapUtil() {
    }

    public static String removeOverlap(String previousContext, String newText) {
        if (newText == null || newText.isBlank()) {
            return "";
        }
        if (previousContext == null || previousContext.isBlank()) {
            return newText.trim();
        }

        List<String> prevTokens = tokenize(previousContext);
        List<String> newTokens = tokenize(newText);
        if (prevTokens.isEmpty() || newTokens.isEmpty()) {
            return newText.trim();
        }

        int maxLen = Math.min(MAX_OVERLAP_WORDS, Math.min(prevTokens.size(), newTokens.size()));
        for (int len = maxLen; len >= MIN_OVERLAP_WORDS; len--) {
            if (suffixEquals(prevTokens, len, newTokens, len)) {
                List<String> remaining = newTokens.subList(len, newTokens.size());
                return remaining.isEmpty() ? "" : String.join(" ", remaining).trim();
            }
        }
        return newText.trim();
    }

    public static String extractContext(String text) {
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return "";
        }
        int from = Math.max(0, tokens.size() - CONTEXT_WORDS);
        return String.join(" ", tokens.subList(from, tokens.size()));
    }

    public static void appendWithSpace(StringBuilder builder, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(text.trim());
    }

    private static List<String> tokenize(String text) {
        return new ArrayList<>(Arrays.stream(text.toLowerCase().trim().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .toList());
    }

    private static boolean suffixEquals(List<String> prevTokens, int len, List<String> newTokens, int compareLen) {
        if (len != compareLen) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!prevTokens.get(prevTokens.size() - len + i).equals(newTokens.get(i))) {
                return false;
            }
        }
        return true;
    }
}
