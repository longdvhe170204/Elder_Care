package com.eldercare.backend.admission.internal.change.domain;

import com.eldercare.backend.common.error.InvalidValueException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Xác nhận của Hành chính với người đại diện về yêu cầu thay đổi lưu trú, kèm bằng chứng (chủ dự án yêu cầu 2026-11-20,
 * thay cho bản scan phụ lục ký của FR-043a): yêu cầu do Hành chính hoặc Bác sĩ lập chỉ gửi Quản lý viện duyệt được khi có
 * xác nhận này. Lưu trong nội dung yêu cầu ở trường {@value #FIELD}; {@code recordedBy}, {@code recordedAt} do hệ thống
 * ghi theo người lưu.
 */
public record RepresentativeConfirmation(
        Method method,
        LocalDate confirmedOn,
        long evidenceFileId,
        Long representativeFamilyMemberId,
        String note,
        Long recordedBy,
        Instant recordedAt) {

    public static final String FIELD = "representativeConfirmation";
    public static final int NOTE_MAX = 500;

    /** Cách xác nhận: gặp trực tiếp (ký phụ lục), gọi điện, tin nhắn. */
    public enum Method {
        IN_PERSON,
        PHONE,
        MESSAGE
    }

    /** Xác nhận trong nội dung yêu cầu; không có thì rỗng, sai hình dạng thì 422. */
    public static Optional<RepresentativeConfirmation> of(JsonNode content) {
        JsonNode node = content == null ? null : content.get(FIELD);
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (!node.isObject()) {
            throw new InvalidValueException("representativeConfirmation phải là object");
        }
        Method method;
        try {
            method = Method.valueOf(node.path("method").asString(""));
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("representativeConfirmation.method cần IN_PERSON, PHONE hoặc MESSAGE");
        }
        LocalDate confirmedOn;
        try {
            confirmedOn = LocalDate.parse(node.path("confirmedOn").asString(""));
        } catch (DateTimeParseException e) {
            throw new InvalidValueException("representativeConfirmation.confirmedOn là ngày bắt buộc");
        }
        JsonNode file = node.get("evidenceFileId");
        if (file == null || !file.isIntegralNumber() || file.asLong() <= 0) {
            throw new InvalidValueException("representativeConfirmation.evidenceFileId là tệp bằng chứng bắt buộc");
        }
        Long representative = node.hasNonNull("representativeFamilyMemberId")
                ? node.get("representativeFamilyMemberId").asLong()
                : null;
        String note = node.hasNonNull("note") ? node.get("note").asString().strip() : null;
        if (note != null && note.length() > NOTE_MAX) {
            throw new InvalidValueException("Ghi chú xác nhận tối đa " + NOTE_MAX + " ký tự");
        }
        Long recordedBy = node.hasNonNull("recordedBy") ? node.get("recordedBy").asLong() : null;
        Instant recordedAt = node.hasNonNull("recordedAt")
                ? Instant.parse(node.get("recordedAt").asString())
                : null;
        return Optional.of(new RepresentativeConfirmation(
                method,
                confirmedOn,
                file.asLong(),
                representative,
                note == null || note.isEmpty() ? null : note,
                recordedBy,
                recordedAt));
    }

    public RepresentativeConfirmation recordedBy(long accountId, Instant at) {
        return new RepresentativeConfirmation(
                method, confirmedOn, evidenceFileId, representativeFamilyMemberId, note, accountId, at);
    }

    /**
     * Đủ để gửi duyệt: có người đại diện đã xác nhận (Q-414, R16). Xác nhận ghi trước khi có trường này coi như thiếu,
     * phải xác nhận lại.
     */
    public boolean names() {
        return representativeFamilyMemberId != null;
    }

    public ObjectNode toJson() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("method", method.name());
        node.put("confirmedOn", confirmedOn.toString());
        node.put("evidenceFileId", evidenceFileId);
        if (representativeFamilyMemberId != null) {
            node.put("representativeFamilyMemberId", representativeFamilyMemberId);
        }
        if (note != null) {
            node.put("note", note);
        }
        if (recordedBy != null) {
            node.put("recordedBy", recordedBy);
        }
        if (recordedAt != null) {
            node.put("recordedAt", recordedAt.toString());
        }
        return node;
    }
}
