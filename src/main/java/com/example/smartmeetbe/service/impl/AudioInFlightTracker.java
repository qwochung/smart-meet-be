package com.example.smartmeetbe.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Đếm số chunk audio đang được ASR xử lý (bất đồng bộ) theo từng room.
 * Dùng để finalize đợi cho tới khi không còn chunk nào đang dịch dở -> tránh merge thiếu dữ liệu.
 *
 * Lưu ý: counter là in-memory nên chỉ đúng khi chạy 1 instance (Cloud Run đang để max-instances=1).
 * Nếu sau này scale nhiều instance thì phải chuyển sang Redis.
 */
@Slf4j
@Component
public class AudioInFlightTracker {

    private final Map<String, AtomicInteger> inFlight = new ConcurrentHashMap<>();

    /** +1 ngay khi WS nhận được chunk (gọi đồng bộ trước khi dispatch async). */
    public void begin(String roomId) {
        inFlight.computeIfAbsent(roomId, k -> new AtomicInteger()).incrementAndGet();
    }

    /** -1 khi chunk đã xử lý xong (dù thành công hay lỗi). Không xóa entry ở đây để tránh race. */
    public void end(String roomId) {
        inFlight.computeIfPresent(roomId, (k, counter) -> {
            counter.decrementAndGet();
            return counter;
        });
    }

    /** Số chunk còn đang xử lý cho room (không bao giờ âm). */
    public int count(String roomId) {
        AtomicInteger counter = inFlight.get(roomId);
        return counter == null ? 0 : Math.max(0, counter.get());
    }

    /**
     * Đợi tới khi không còn chunk nào đang xử lý cho room, hoặc hết timeout.
     * @return true nếu đã drain hết, false nếu hết giờ mà vẫn còn chunk đang chạy.
     */
    public boolean awaitDrained(String roomId, long timeoutMs, long pollMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (count(roomId) > 0) {
            if (System.currentTimeMillis() >= deadline) {
                log.warn("In-flight drain timeout for room {} after {}ms, still {} chunk(s) processing",
                        roomId, timeoutMs, count(roomId));
                return false;
            }
            try {
                Thread.sleep(pollMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /** Dọn entry khi room đã finalize xong (tránh leak map theo số room). */
    public void clear(String roomId) {
        inFlight.remove(roomId);
    }
}
