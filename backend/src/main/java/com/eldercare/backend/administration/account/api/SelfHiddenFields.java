package com.eldercare.backend.administration.account.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;

/**
 * DTO có trường mà module tự kiểm quyền (không gắn được {@link FieldGroup} vì quyền không theo nhóm thông tin, ví dụ cờ
 * {@code hasTreatmentWish} của 001 FR-023m) cài interface này: giới hạn trường của 002 bỏ các trường được trả khỏi phản
 * hồi và ghi tên vào {@code hiddenFields} như trường bị ẩn theo nhóm (002 research A-08; "Đổi api" 2026-10-24).
 */
public interface SelfHiddenFields {

    /** Tên thuộc tính JSON người xem hiện tại không được thấy; rỗng là không ẩn thêm. */
    @JsonIgnore
    Set<String> selfHiddenFields();
}
