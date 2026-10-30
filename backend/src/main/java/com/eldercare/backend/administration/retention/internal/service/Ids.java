package com.eldercare.backend.administration.retention.internal.service;

import java.util.Collection;
import java.util.stream.Collectors;

/** Danh sách id số cho mệnh đề {@code IN}; id là {@code Long} nên ghép chuỗi an toàn. */
final class Ids {

    private Ids() {}

    /** Chuỗi {@code ?, ?, …} cho mệnh đề {@code IN} có tham số. */
    static String placeholders(int n) {
        return String.join(", ", java.util.Collections.nCopies(n, "?"));
    }

    static String inList(Collection<Long> ids) {
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Danh sách id rỗng");
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
