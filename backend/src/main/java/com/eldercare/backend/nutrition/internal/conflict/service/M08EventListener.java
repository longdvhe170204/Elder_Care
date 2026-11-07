package com.eldercare.backend.nutrition.internal.conflict.service;

import com.eldercare.backend.common.data.ReminderMarkers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khung listener idempotent của M08 (research M-08; data-model mục 0.1 {@code M08.PROCESSED_EVENT}): mỗi {@code khoa
 * sự kiện} xử lý đúng một lần. Listener cụ thể (dị ứng, vắng, chuyển giường…) bọc việc của mình trong {@link #once};
 * việc lỗi thì dấu cũng hoàn tác nên outbox giao lại sẽ chạy lại. Việc chỉ xét bữa đã chốt chưa tới giờ do listener tự lọc.
 */
@Component
public class M08EventListener {

    private static final String KIND = "M08.PROCESSED_EVENT";
    private static final String ENTITY_TYPE = "EVENT";
    private static final int MAX_MARKER = 80;

    private final ReminderMarkers markers;

    public M08EventListener(ReminderMarkers markers) {
        this.markers = markers;
    }

    @Transactional
    public void once(String eventKey, Runnable work) {
        if (markers.mark(KIND, ENTITY_TYPE, 0L, marker(eventKey), null)) {
            work.run();
        }
    }

    /** Khóa dài hơn cột {@code marker} (80) thì dùng SHA-256 hex của khóa. */
    private static String marker(String eventKey) {
        if (eventKey.length() <= MAX_MARKER) {
            return eventKey;
        }
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(eventKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
