package com.cambae.backend.m04.api.events;

import com.cambae.backend.m04.api.dto.CareEventContext;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

/** NFR-04: loại + id + mốc; thay thời điểm giao lại không đổi khóa. */
public interface CareEvent {
    CareEventContext context();

    default String keyNamespace() {
        return context().sourceType();
    }

    default String eventKey() {
        try {
            return getClass().getSimpleName() + ":" + context().sourceId() + ":"
                    + HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256")
                                    .digest((keyNamespace() + "\u0000"
                                                    + context().milestone())
                                            .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK thiếu SHA-256", e);
        }
    }
}
