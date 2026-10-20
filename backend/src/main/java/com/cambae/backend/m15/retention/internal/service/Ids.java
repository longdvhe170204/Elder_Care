package com.cambae.backend.m15.retention.internal.service;

import java.util.Collection;
import java.util.stream.Collectors;

/** Danh sách id số cho mệnh đề {@code IN}; id là {@code Long} nên ghép chuỗi an toàn. */
final class Ids {

    private Ids() {}

    static String inList(Collection<Long> ids) {
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Danh sách id rỗng");
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
