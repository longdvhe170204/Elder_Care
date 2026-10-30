package com.eldercare.backend.care.api.dto;

import com.eldercare.backend.common.Role;
import java.util.Set;

/** Vai trò và loại giấy phép/đào tạo yêu cầu của một loại công việc; role null: không giới hạn vai trò. */
public record TaskRequirements(Role role, Set<String> certificates) {
    public TaskRequirements {
        certificates = Set.copyOf(certificates);
    }
}
