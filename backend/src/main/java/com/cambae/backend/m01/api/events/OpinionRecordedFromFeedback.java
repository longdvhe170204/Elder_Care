package com.cambae.backend.m01.api.events;

import com.cambae.backend.m01.api.dto.OpinionSource;

/**
 * Module sở hữu phát khi lệnh ghi mục ý kiến có {@code feedbackId} (phản hồi loại Mong muốn); 012 nghe để gắn mục vào
 * phản hồi nếu phản hồi thuộc đúng người cao tuổi và nguồn là {@code RESIDENT} (FR-023g, Q-366; research H-17).
 *
 * <p>Bộ giá trị đã chốt (001 T076): chỉ 014 phát, từ module {@link #SOURCE_ACTIVITY_MODULE} (m04, do C làm), loại mục
 * {@link #ITEM_INTEREST} (bảng {@code so_thich}; 012 lưu thành {@code loai_muc SO_THICH}). Thêm module hoặc loại mục
 * khác thì sửa ở đây cùng contracts/module-interfaces.md mục 1.8b và báo 012.
 *
 * @param feedbackId mã phản hồi Mong muốn của 012
 * @param sourceModule module sở hữu mục ý kiến ({@link #SOURCE_ACTIVITY_MODULE})
 * @param itemType loại mục ({@link #ITEM_INTEREST})
 * @param itemId mã mục ý kiến trong module nguồn
 */
public record OpinionRecordedFromFeedback(
        long feedbackId, String sourceModule, String itemType, long itemId, long residentId, OpinionSource source) {

    /** Module M04, nơi 014 ghi mục sở thích. */
    public static final String SOURCE_ACTIVITY_MODULE = "M04";

    /** Mục sở thích của người cao tuổi ({@code so_thich} của 014). */
    public static final String ITEM_INTEREST = "INTEREST";

    public OpinionRecordedFromFeedback {
        if (!SOURCE_ACTIVITY_MODULE.equals(sourceModule)) {
            throw new IllegalArgumentException("sourceModule phải là " + SOURCE_ACTIVITY_MODULE + ": " + sourceModule);
        }
        if (!ITEM_INTEREST.equals(itemType)) {
            throw new IllegalArgumentException("itemType phải là " + ITEM_INTEREST + ": " + itemType);
        }
        if (feedbackId <= 0 || itemId <= 0 || residentId <= 0) {
            throw new IllegalArgumentException("feedbackId, itemId, residentId phải dương");
        }
        if (source == null) {
            throw new IllegalArgumentException("source là bắt buộc");
        }
    }
}
