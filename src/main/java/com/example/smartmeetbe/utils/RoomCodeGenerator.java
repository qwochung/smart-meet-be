package com.example.smartmeetbe.utils;

import java.security.SecureRandom;

public class RoomCodeGenerator {

    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        return randomPart(3) + "-" + randomPart(4) + "-" + randomPart(3);
    }

    private static String randomPart(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}