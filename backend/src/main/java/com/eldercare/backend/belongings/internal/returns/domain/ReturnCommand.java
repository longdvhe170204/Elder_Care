package com.eldercare.backend.belongings.internal.returns.domain;

import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
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

    /** Một đồ: tình trạng lúc trả (bắt buộc), ảnh. */
    public record Item(
            long belongingId,
            int version,
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
