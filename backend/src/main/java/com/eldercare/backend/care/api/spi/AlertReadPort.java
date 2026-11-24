package com.eldercare.backend.care.api.spi;

import java.util.*;

/** Nguồn cảnh báo đang mở của 007 cho checklist/bàn giao. */
public interface AlertReadPort {
    List<AlertView> open(Collection<Long> residentIds);

    Map<Long, List<Long>> byTask(Collection<Long> taskIds);

    /** {@code type}: loại cảnh báo (api-007 {@code Alert.type}) cho nhãn "Đang theo dõi" (005 FR-057, Q-443). */
    record AlertView(long id, long residentId, String type) {}
}
