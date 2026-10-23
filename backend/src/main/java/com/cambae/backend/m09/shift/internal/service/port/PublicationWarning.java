package com.cambae.backend.m09.shift.internal.service.port;

import java.util.Map;

/** Cảnh báo mở rộng khi công bố, dùng cùng tên trường với schema Warning; không phải điều kiện chặn. */
public record PublicationWarning(String code, String description, Map<String, Object> details) {

    public PublicationWarning {
        details = Map.copyOf(details);
    }
}
