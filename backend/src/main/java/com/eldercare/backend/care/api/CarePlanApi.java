package com.eldercare.backend.care.api;

import java.util.Set;

/** KeHoachChamSocApi.loaiCongViecHienHanh: loại công việc trong phiên bản Hiệu lực. */
public interface CarePlanApi {
    Set<Long> currentTaskTypes(long residentId);
}
