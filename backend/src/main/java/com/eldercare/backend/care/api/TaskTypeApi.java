package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.TaskRequirements;

/** LoaiCongViecApi.yeuCau của 005 mục 1.2; không có danh mục thì không mặc định đủ điều kiện. */
public interface TaskTypeApi {
    TaskRequirements requirements(long taskTypeId);
}
