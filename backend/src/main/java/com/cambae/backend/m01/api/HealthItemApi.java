package com.cambae.backend.m01.api;

import com.cambae.backend.m01.api.dto.HealthItemCheckSource;
import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.api.dto.HealthItemView;
import java.util.List;

/** Mục sức khỏe (dị ứng, bệnh nền, tiền sử) cho 006, 007, 011. */
public interface HealthItemApi {

    /** Mục Hiệu lực, kèm cờ không kiểm tra tự động và chờ kiểm tra; không truyền loại thì trả mọi loại. */
    List<HealthItemView> active(long residentId, HealthItemType... types);

    /** 006 (MEDICATION), 011 (NUTRITION) gỡ cờ chờ kiểm tra của mình (BR-M01-12). */
    void markChecked(long itemId, HealthItemCheckSource source);
}
