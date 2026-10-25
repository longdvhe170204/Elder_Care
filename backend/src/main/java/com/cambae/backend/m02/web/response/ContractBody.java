package com.cambae.backend.m02.web.response;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.domain.ContractContent;
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
import java.util.List;

/**
 * Hợp đồng (contracts/api-004.yaml, schema {@code Contract}; FR-019, FR-028). Nội dung tài chính (đơn giá, khoản cọc, dòng
 * ghi đè chính sách vắng) thuộc nhóm {@code FINANCE}: 002 bỏ khỏi phản hồi khi người xem không được xem và ghi tên vào {@code
 * hiddenFields}. {@code target} chỉ để xét phạm vi, không ra ngoài.
 */
@Schema(name = "Contract")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContractBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long residentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ResidenceType residenceType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDate,
        LocalDate endDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CareLevel careLevel,
        Long roomTypeId,
        Long bedId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FeeBillingPeriod residencyFeeBillingPeriod,
        @FieldGroup(InfoGroup.FINANCE) List<ServiceLineBody> services,
        @FieldGroup(InfoGroup.FINANCE) Boolean noDepositRequired,
        @FieldGroup(InfoGroup.FINANCE) Long depositAmount,
        String bedHoldPolicy,
        @FieldGroup(InfoGroup.FINANCE) List<AbsencePolicyLineBody> absencePolicyOverrides,
        List<ArrivalLineBody> arrivalSchedule,
        String terminationCondition,
        long id,
        String contractNumber,
        ContractStatus status,
        LocalDate currentEndDate,
        boolean awaitingTermsApproval,
        LocalDate overdueSince,
        LocalDate signedDate,
        String terminationReason,
        List<String> hiddenFields,
        List<String> allowedCommands,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    public static ContractBody of(Contract contract) {
        ContractContent c = contract.content();
        return new ContractBody(
                contract.residentId(),
                c.residenceType(),
                c.startDate(),
                c.endDate(),
                c.careLevel(),
                c.roomTypeId(),
                c.bedId(),
                FeeBillingPeriod.valueOf(c.residencyFeeBillingPeriod().name()),
                services(c),
                c.noDepositRequired(),
                money(c.depositAmount()),
                c.bedHoldPolicy(),
                policies(c),
                schedule(c),
                c.terminationCondition(),
                contract.id(),
                contract.contractNumber(),
                contract.status(),
                contract.currentEndDate(),
                contract.awaitingTermsApproval(),
                contract.overdueSince(),
                contract.signedDate(),
                contract.terminationReason(),
                List.of(),
                allowedCommands(contract),
                ContractService.target(contract.id(), contract.residentId()));
    }

    /** Lệnh còn dùng được theo trạng thái (quyền của người dùng do từng lệnh kiểm). */
    static List<String> allowedCommands(Contract contract) {
        return switch (contract.status()) {
            case DRAFT ->
                contract.awaitingTermsApproval()
                        ? List.of("cancel")
                        : List.of("update", "submit-terms-for-approval", "send-for-signature", "cancel");
            case PENDING_SIGNATURE -> List.of("return-to-draft", "record-signed", "cancel");
            default -> List.of();
        };
    }

    static List<ServiceLineBody> services(ContractContent c) {
        return c.services().stream()
                .map(line -> new ServiceLineBody(
                        line.serviceId(), line.inPackage(), line.quantity(), money(line.customUnitPrice())))
                .toList();
    }

    static List<AbsencePolicyLineBody> policies(ContractContent c) {
        return c.absencePolicyOverrides().stream()
                .map(line -> new AbsencePolicyLineBody(
                        line.residenceType(),
                        line.absenceType(),
                        line.fromDay(),
                        line.toDay(),
                        line.coefficient(),
                        line.holdBed() == null ? null : HoldBed.valueOf(line.holdBed())))
                .toList();
    }

    static List<ArrivalLineBody> schedule(ContractContent c) {
        return c.arrivalSchedule().stream()
                .map(line -> new ArrivalLineBody(
                        line.dayOfWeek(),
                        line.date(),
                        line.arrivalTime().toString(),
                        line.departureTime().toString()))
                .toList();
    }

    static Long money(BigDecimal amount) {
        return amount == null ? null : amount.longValueExact();
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }
}
