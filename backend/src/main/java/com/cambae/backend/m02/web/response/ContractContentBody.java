package com.cambae.backend.m02.web.response;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.ContractContentView;
import com.cambae.backend.m02.internal.contract.service.ContractService;
import com.cambae.backend.m02.web.request.ContractInputBody.AbsencePolicyLineBody;
import com.cambae.backend.m02.web.request.ContractInputBody.ArrivalLineBody;
import com.cambae.backend.m02.web.request.ContractInputBody.FeeBillingPeriod;
import com.cambae.backend.m02.web.request.ContractInputBody.HoldBed;
import com.cambae.backend.m02.web.request.ContractInputBody.ServiceLineBody;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Nội dung hợp đồng hiệu lực tại một ngày (contracts/api-004.yaml, schema {@code ContractContent}; FR-024, FR-036). Đơn giá
 * đã ghi và các trường tài chính của nội dung thuộc nhóm {@code FINANCE}; hợp đồng api-004 không khai {@code hiddenFields} ở
 * schema này nên trường bị ẩn chỉ đơn giản vắng mặt.
 */
@Schema(name = "ContractContent")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContractContentBody(
        long contractId,
        int version,
        LocalDate effectiveFrom,
        Long addendumId,
        Content content,
        @FieldGroup(InfoGroup.FINANCE) List<RecordedUnitPrice> recordedUnitPrices,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    public record RecordedUnitPrice(long serviceId, long unitPrice) {}

    /** Nội dung theo schema {@code ContractInput}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long residentId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            ResidenceType residenceType,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate startDate,

            LocalDate endDate,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            CareLevel careLevel,

            Long roomTypeId,
            Long bedId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            FeeBillingPeriod residencyFeeBillingPeriod,

            @FieldGroup(InfoGroup.FINANCE) List<ServiceLineBody> services,
            @FieldGroup(InfoGroup.FINANCE) Boolean noDepositRequired,
            @FieldGroup(InfoGroup.FINANCE) Long depositAmount,
            String bedHoldPolicy,
            @FieldGroup(InfoGroup.FINANCE) List<AbsencePolicyLineBody> absencePolicyOverrides,
            List<ArrivalLineBody> arrivalSchedule,
            String terminationCondition,
            @JsonIgnore @Schema(hidden = true) Target target)
            implements VisibilityTarget {

        @Override
        @JsonIgnore
        public Target visibilityTarget() {
            return target;
        }
    }

    public static ContractContentBody of(ContractContentView view, long residentId) {
        JsonNode c = view.content();
        Target target = ContractService.target(view.contractId(), residentId);
        Content content = new Content(
                residentId,
                ResidenceType.valueOf(c.get("residenceType").asString()),
                LocalDate.parse(c.get("startDate").asString()),
                date(c.get("endDate")),
                CareLevel.valueOf(c.get("careLevel").asString()),
                longOrNull(c.get("roomTypeId")),
                longOrNull(c.get("bedId")),
                FeeBillingPeriod.valueOf(c.get("residencyFeeBillingPeriod").asString()),
                services(c.get("services")),
                c.get("noDepositRequired").asBoolean(),
                longOrNull(c.get("depositAmount")),
                text(c.get("bedHoldPolicy")),
                policies(c.get("absencePolicyOverrides")),
                schedule(c.get("arrivalSchedule")),
                text(c.get("terminationCondition")),
                target);
        List<RecordedUnitPrice> recorded = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : view.recordedUnitPrices().entrySet()) {
            recorded.add(new RecordedUnitPrice(entry.getKey(), entry.getValue().longValueExact()));
        }
        return new ContractContentBody(
                view.contractId(), view.version(), view.effectiveFrom(), view.addendumId(), content, recorded, target);
    }

    private static List<ServiceLineBody> services(JsonNode lines) {
        List<ServiceLineBody> result = new ArrayList<>();
        for (JsonNode line : lines) {
            result.add(new ServiceLineBody(
                    line.get("serviceId").asLong(),
                    line.get("inPackage").asBoolean(),
                    line.get("quantity").decimalValue(),
                    longOrNull(line.get("customUnitPrice"))));
        }
        return result;
    }

    private static List<AbsencePolicyLineBody> policies(JsonNode lines) {
        List<AbsencePolicyLineBody> result = new ArrayList<>();
        for (JsonNode line : lines) {
            JsonNode hold = line.get("holdBed");
            result.add(new AbsencePolicyLineBody(
                    ResidenceType.valueOf(line.get("residenceType").asString()),
                    line.get("absenceType").asString(),
                    intOrNull(line.get("fromDay")),
                    intOrNull(line.get("toDay")),
                    line.get("coefficient").asString(),
                    hold == null || hold.isNull() ? null : HoldBed.valueOf(hold.asString())));
        }
        return result;
    }

    private static List<ArrivalLineBody> schedule(JsonNode lines) {
        List<ArrivalLineBody> result = new ArrayList<>();
        for (JsonNode line : lines) {
            result.add(new ArrivalLineBody(
                    intOrNull(line.get("dayOfWeek")),
                    date(line.get("date")),
                    line.get("arrivalTime").asString(),
                    line.get("departureTime").asString()));
        }
        return result;
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString();
    }

    private static Long longOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }

    private static Integer intOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private static LocalDate date(JsonNode node) {
        return node == null || node.isNull() ? null : LocalDate.parse(node.asString());
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }
}
