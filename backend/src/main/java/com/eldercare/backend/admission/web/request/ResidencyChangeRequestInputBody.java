package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.admission.internal.change.domain.ChangeType;
import com.eldercare.backend.admission.internal.change.domain.RepresentativeConfirmation;
import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Thân lập yêu cầu thay đổi lưu trú (contracts/api-004.yaml, schema {@code ResidencyChangeRequestInput}; FR-037, FR-038).
 * {@code newValue} theo loại: {@code residenceType}, {@code careLevel}, {@code services[]} ({@code serviceId}, {@code
 * action} ADD hoặc REMOVE), {@code endDate}, {@code newBedId}, {@code arrivalSchedule[]}; kèm khoản cọc mới khi đổi.
 */
@Schema(name = "ResidencyChangeRequestInput")
public record ResidencyChangeRequestInputBody(
        @Schema(description = "Hợp đồng Hiệu lực; không gửi thì gửi residentId")
        Long contractId,

        @Schema(
                description =
                        "Người cao tuổi, thay cho contractId khi người lập không xem được hợp đồng (Bác sĩ): hệ thống"
                                + " lấy hợp đồng Hiệu lực của người đó")
        Long residentId,

        @NotEmpty @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<ChangeType> changeTypes,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        JsonNode newValue,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate requestedEffectiveDate,

        @Valid Reference reference,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason,

        @Valid
        @Schema(
                description = "Hành chính xác nhận với người đại diện, kèm bằng chứng; bắt buộc trước khi gửi duyệt"
                        + " yêu cầu do Hành chính, Bác sĩ lập")
        RepresentativeConfirmationBody representativeConfirmation) {

    /**
     * Xác nhận với người đại diện (2026-11-20): cách xác nhận, ngày, tệp bằng chứng (PDF, JPG, PNG đã tải lên), ghi chú;
     * {@code recordedBy}, {@code recordedAt} do hệ thống ghi.
     */
    @Schema(name = "ResidencyChangeRepresentativeConfirmation")
    public record RepresentativeConfirmationBody(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            RepresentativeConfirmation.Method method,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate confirmedOn,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long evidenceFileId,

            @NotNull
            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    description = "Người đại diện đã xác nhận, là người đại diện hiện tại tại ngày xác nhận (Q-415)")
            Long representativeFamilyMemberId,

            @Size(max = RepresentativeConfirmation.NOTE_MAX) String note,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            String recordedBy,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            Instant recordedAt) {}

    /** Lần đánh giá (BR-M01-03) hoặc lệnh chuyển giường gấp (003 FR-030) làm căn cứ. */
    @Schema(name = "ResidencyChangeReference")
    public record Reference(Type type, Long id) {

        public enum Type {
            ASSESSMENT,
            BED_TRANSFER_ORDER
        }
    }

    /** Nội dung yêu cầu phê duyệt (không kèm phần hệ thống điền: giá trị trước, ảnh hưởng chi phí). */
    /** Nội dung yêu cầu với hợp đồng đã xác định (từ {@code contractId}, hoặc hợp đồng Hiệu lực của {@code residentId}). */
    public JsonNode toContent(long contractId) {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("contractId", contractId);
        var types = content.putArray("changeTypes");
        changeTypes.forEach(type -> types.add(type.name()));
        content.set("newValue", newValue);
        if (reference != null) {
            ObjectNode ref = content.putObject("reference");
            // Thiếu type hoặc id thì ResidencyChangeContent.parse trả 422 (hợp đồng không đánh dấu hai trường này bắt
            // buộc)
            ref.put("type", reference.type() == null ? null : reference.type().name());
            ref.put("id", reference.id());
        }
        if (representativeConfirmation != null) {
            ObjectNode confirmation = content.putObject("representativeConfirmation");
            confirmation.put(
                    "method",
                    representativeConfirmation.method() == null
                            ? null
                            : representativeConfirmation.method().name());
            confirmation.put(
                    "confirmedOn",
                    representativeConfirmation.confirmedOn() == null
                            ? null
                            : representativeConfirmation.confirmedOn().toString());
            confirmation.put("evidenceFileId", representativeConfirmation.evidenceFileId());
            confirmation.put("representativeFamilyMemberId", representativeConfirmation.representativeFamilyMemberId());
            confirmation.put("note", representativeConfirmation.note());
        }
        return content;
    }
}
