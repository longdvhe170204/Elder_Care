package com.eldercare.backend.belongings.internal.returns.domain;

import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Lệnh Trả một hoặc nhiều đồ cho cùng người nhận ({@code LenhTra}; api-013 {@code ReturnCommand}; FR-016 → FR-019).
 */
public record ReturnCommand(
        List<Item> items,
        Recipient recipient,
        Long recipientConfirmationId,
        Long signatureFileId,
        String noSignatureReason,
        String conditionOpinion) {

    public ReturnCommand {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * Một đồ: số lượng trả (số tiền với tiền mặt; {@code null} là toàn bộ, nhỏ hơn hiện có thì phần trả tách thành đồ mới
     * theo FR-012), tình trạng lúc trả (bắt buộc), ảnh.
     */
    public record Item(
            long belongingId,
            int version,
            BigDecimal amount,
            Long conditionLevelId,
            String conditionDescription,
            List<Long> photoFileIds) {

        public Item {
            photoFileIds = photoFileIds == null ? List.of() : List.copyOf(photoFileIds);
        }
    }

    /** Người nhận do nhân viên khai; giấy tờ là giấy tờ xuất trình (FR-016). */
    public record Recipient(
            GiverType type, Long familyMemberId, String fullName, String documentType, String documentNumber) {}
}
