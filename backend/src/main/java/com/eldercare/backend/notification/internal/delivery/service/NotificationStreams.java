package com.eldercare.backend.notification.internal.delivery.service;

import com.eldercare.backend.common.notification.NotificationLevel;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Luồng Server-Sent Events của web (research B-05): mỗi tài khoản có thể mở nhiều tab; thông báo mới vào hộp thì đẩy một sự
 * kiện {@code notification} mang id và mức, web tải lại hộp. Mất kết nối thì web dùng dự phòng polling.
 */
@Component
public class NotificationStreams {

    /** Thời gian giữ một luồng, mili giây (thông số kỹ thuật, không phải mốc nghiệp vụ); hết thì trình duyệt mở lại. */
    static final long TIMEOUT_MILLIS = 30L * 60 * 1000;

    private final Map<Long, Set<SseEmitter>> byAccount = new ConcurrentHashMap<>();

    public SseEmitter open(long accountId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        Set<SseEmitter> emitters = byAccount.computeIfAbsent(accountId, k -> ConcurrentHashMap.newKeySet());
        emitters.add(emitter);
        Runnable remove = () -> emitters.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        // Dòng chú thích để trình duyệt báo "đã mở" ngay, không chờ tới thông báo đầu tiên; web bỏ qua dòng này.
        try {
            emitter.send(SseEmitter.event().comment("open"));
        } catch (IOException e) {
            emitters.remove(emitter);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /** Đẩy thông báo mới tới mọi luồng đang mở của tài khoản; luồng lỗi bị bỏ. */
    public void publish(long accountId, long notificationId, NotificationLevel level) {
        Set<SseEmitter> emitters = byAccount.getOrDefault(accountId, Set.of());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(Map.of("notificationId", notificationId, "level", level.name())));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
