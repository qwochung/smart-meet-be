package com.example.smartmeetbe.constant;

public enum RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY;

    /** Nhãn tiếng Việt hiển thị cho người dùng. */
    public String label() {
        return switch (this) {
            case DAILY -> "Lặp hằng ngày";
            case WEEKLY -> "Lặp hằng tuần";
            case MONTHLY -> "Lặp hằng tháng";
            default -> null;
        };
    }
}
