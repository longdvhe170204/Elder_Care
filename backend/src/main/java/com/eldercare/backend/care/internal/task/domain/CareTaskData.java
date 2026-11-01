package com.eldercare.backend.care.internal.task.domain;

import java.util.Set;

/** Snapshot nội dung/điều kiện tại lúc lập, không đổi khi danh mục được sửa. */
public record CareTaskData(
        long publicTaskTypeId,
        String taskTypeName,
        String content,
        String resultType,
        boolean billable,
        Long serviceId,
        Set<String> certificates) {
    public CareTaskData {
        certificates = Set.copyOf(certificates);
    }
}
