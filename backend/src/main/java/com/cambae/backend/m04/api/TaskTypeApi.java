package com.cambae.backend.m04.api;

import com.cambae.backend.m04.api.dto.TaskRequirements;

/** LoaiCongViecApi.yeuCau của 005 mục 1.2; không có danh mục thì không mặc định đủ điều kiện. */
public interface TaskTypeApi {
    TaskRequirements requirements(long taskTypeId);
}
