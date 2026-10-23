package com.cambae.backend.m01.api;

import com.cambae.backend.m01.api.dto.ConsentView;
import java.util.Optional;

/** Bản đồng ý chia sẻ dữ liệu cho 009, 012 và điều kiện chú thích "1" của 002 (BR-M01-08, DBR-03). */
public interface ConsentApi {

    /** Phạm vi người thân, đồng ý hình ảnh, đồng ý thông báo không khẩn (Q-353). */
    Optional<ConsentView> current(long residentId);

    /** Bản đồng ý Hiệu lực bao gồm người thân đó. */
    boolean includes(long residentId, long familyMemberId);

    /** Người thân ký bản đồng ý Hiệu lực với {@code LEGAL_REPRESENTATIVE} (012 FR-020c). */
    Optional<Long> legalRepresentative(long residentId);
}
