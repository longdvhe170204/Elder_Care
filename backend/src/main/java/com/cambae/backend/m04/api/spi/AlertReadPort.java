package com.cambae.backend.m04.api.spi;

import java.util.*;

/** Nguồn cảnh báo đang mở của 007 cho checklist/bàn giao. */
public interface AlertReadPort {
    List<AlertView> open(Collection<Long> residentIds);

    Map<Long, List<Long>> byTask(Collection<Long> taskIds);

    record AlertView(long id, long residentId) {}
}
