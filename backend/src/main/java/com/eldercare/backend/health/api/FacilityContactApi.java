package com.eldercare.backend.health.api;

import com.eldercare.backend.health.api.dto.FacilityContactView;

/** Đọc số liên hệ hiện hành của viện cho quy trình khẩn cấp (007 mục 1.4). */
public interface FacilityContactApi {
    FacilityContactView current();
}
