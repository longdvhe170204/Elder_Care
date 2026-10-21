package com.cambae.backend.m13;

import com.cambae.backend.m13.internal.delivery.service.PushGateway;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * FCM giả (research B-13): ghi lại lần đẩy và thời điểm nhận; token trong {@link #invalid} trả token hết hạn; token bắt
 * đầu bằng {@code slow-} chờ tới khi test mở {@link #release}.
 */
class FakePushGateway implements PushGateway {

    record Push(String token, Message message, Instant at) {}

    final List<Push> pushes = java.util.Collections.synchronizedList(new ArrayList<>());
    final Set<String> invalid = ConcurrentHashMap.newKeySet();
    volatile CountDownLatch release = new CountDownLatch(0);

    void reset() {
        pushes.clear();
        invalid.clear();
        release = new CountDownLatch(0);
    }

    List<Push> pushesTo(String token) {
        synchronized (pushes) {
            return pushes.stream().filter(p -> p.token().equals(token)).toList();
        }
    }

    @Override
    public Outcome push(String token, Message message) {
        if (token.startsWith("slow-")) {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        pushes.add(new Push(token, message, Instant.now()));
        return invalid.contains(token) ? Outcome.INVALID_TOKEN : Outcome.DELIVERED;
    }
}
