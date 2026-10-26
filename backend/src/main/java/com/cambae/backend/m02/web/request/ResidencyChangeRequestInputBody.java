package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m02.internal.change.domain.ChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long contractId,

        @NotEmpty @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<ChangeType> changeTypes,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        JsonNode newValue,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate requestedEffectiveDate,

        @Valid Reference reference,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {

    /** Lần đánh giá (BR-M01-03) hoặc lệnh chuyển giường gấp (003 FR-030) làm căn cứ. */
    @Schema(name = "ResidencyChangeReference")
    public record Reference(Type type, Long id) {

        public enum Type {
            ASSESSMENT,
            BED_TRANSFER_ORDER
        }
    }

    /** Nội dung yêu cầu phê duyệt (không kèm phần hệ thống điền: giá trị trước, ảnh hưởng chi phí). */
    public JsonNode toContent() {
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
        return content;
    }
}
