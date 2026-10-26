package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidenceTypePeriod;
import com.cambae.backend.m01.api.spi.ContractApi;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ArrivalScheduleView;
import com.cambae.backend.m02.api.dto.ContractContentView;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.dto.DepositAmountView;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.domain.ContractVersion;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Đọc nội dung hợp đồng cho module khác và cho endpoint {@code /contracts/{id}/content} (FR-024, BR-M02-09; research T-04):
 * nội dung tại ngày D là phiên bản có {@code effective_from} lớn nhất không muộn hơn D của hợp đồng đã ký (Hiệu lực, Kết
 * thúc, Chấm dứt), nên không phải ghép phụ lục mỗi lần đọc. Hợp đồng Hiệu lực qua ngày kết thúc vẫn dùng điều khoản cuối cùng
 * (FR-029). Cài thêm {@link ContractApi} của 001 (loại lưu trú theo hợp đồng, FR-009, FR-052, FR-053).
 */
@Service
public class ContractReadService implements ContractReadApi, ContractApi {

    private static final Set<ContractStatus> SIGNED =
            Set.of(ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED);

    private final ContractStore store;

    ContractReadService(ContractStore store) {
        this.store = store;
    }

    /** Nội dung của một hợp đồng đã ký tại ngày; hợp đồng chưa ký hoặc ngày trước ngày bắt đầu thì 404. */
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public ContractContentView contentOf(Target target, long contractId, LocalDate date) {
        Contract contract = store.find(contractId).orElseThrow(NotFoundException::new);
        if (!SIGNED.contains(contract.status())) {
            throw new NotFoundException();
        }
        return store.versionAt(contractId, date).map(this::view).orElseThrow(NotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContractContentView> contentAtDate(long residentId, LocalDate date) {
        return coveringContract(residentId, date)
                .flatMap(c -> store.versionAt(c.id(), date))
                .map(this::view);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContractView> contract(long contractId) {
        return store.find(contractId).map(this::view);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractView> ofResident(long residentId) {
        return store.ofResident(residentId).stream().map(this::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DepositAmountView> depositAmount(long contractId) {
        return store.find(contractId).map(c -> {
            // Phụ lục có thể đổi khoản cọc (FR-030): khoản hiện hành là của phiên bản nội dung mới nhất
            Optional<ContractVersion> latest = store.latestVersion(c.id());
            if (latest.isPresent()) {
                JsonNode content = latest.get().content();
                return new DepositAmountView(
                        c.id(),
                        content.path("noDepositRequired").asBoolean(false),
                        content.hasNonNull("depositAmount")
                                ? content.get("depositAmount").decimalValue()
                                : BigDecimal.ZERO);
            }
            return new DepositAmountView(
                    c.id(),
                    c.content().noDepositRequired(),
                    c.content().depositAmount() == null
                            ? BigDecimal.ZERO
                            : c.content().depositAmount());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArrivalScheduleView> arrivalScheduleAtDate(long residentId, LocalDate date) {
        Optional<Contract> covering =
                coveringContract(residentId, date).filter(c -> c.status() == ContractStatus.ACTIVE);
        if (covering.isEmpty()) {
            return List.of();
        }
        Contract contract = covering.get();
        Optional<ContractVersion> version = store.versionAt(contract.id(), date);
        if (version.isEmpty()
                || !ResidenceType.DAY_CARE
                        .name()
                        .equals(version.get().content().get("residenceType").asString())) {
            return List.of();
        }
        List<ArrivalScheduleView> lines = new ArrayList<>();
        for (JsonNode line : version.get().content().get("arrivalSchedule")) {
            boolean weekly = !line.get("dayOfWeek").isNull()
                    && line.get("dayOfWeek").asInt() == date.getDayOfWeek().getValue();
            boolean dated = !line.get("date").isNull()
                    && LocalDate.parse(line.get("date").asString()).equals(date);
            if (weekly || dated) {
                lines.add(new ArrivalScheduleView(
                        contract.id(),
                        LocalTime.parse(line.get("arrivalTime").asString()),
                        LocalTime.parse(line.get("departureTime").asString())));
            }
        }
        return lines;
    }

    // ---- ContractApi của 001 --------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Optional<ResidenceType> currentResidenceType(long residentId, LocalDate date) {
        return residenceTypeHistory(residentId).stream()
                .filter(period -> !period.from().isAfter(date)
                        && (period.to().isEmpty() || !period.to().get().isBefore(date)))
                .max(Comparator.comparing(ResidenceTypePeriod::from))
                .map(ResidenceTypePeriod::type);
    }

    /**
     * Mỗi phiên bản nội dung của hợp đồng đã ký là một khoảng ngày từ {@code effective_from} tới ngày liền trước phiên bản
     * kế; khoảng cuối tới ngày kết thúc hiện hành, để trống nếu hợp đồng Hiệu lực không có ngày kết thúc hoặc đã quá hạn
     * (FR-029: điều khoản cuối vẫn áp dụng).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ResidenceTypePeriod> residenceTypeHistory(long residentId) {
        List<ResidenceTypePeriod> periods = new ArrayList<>();
        for (Contract contract : store.ofResident(residentId)) {
            if (!SIGNED.contains(contract.status())) {
                continue;
            }
            List<ContractVersion> versions = store.versions(contract.id());
            for (int i = 0; i < versions.size(); i++) {
                ContractVersion version = versions.get(i);
                Optional<LocalDate> to = i + 1 < versions.size()
                        ? Optional.of(versions.get(i + 1).effectiveFrom().minusDays(1))
                        : lastEnd(contract);
                periods.add(new ResidenceTypePeriod(
                        ResidenceType.valueOf(
                                version.content().get("residenceType").asString()),
                        version.effectiveFrom(),
                        to));
            }
        }
        periods.sort(Comparator.comparing(ResidenceTypePeriod::from));
        return periods;
    }

    private static Optional<LocalDate> lastEnd(Contract contract) {
        if (contract.status() == ContractStatus.ACTIVE && contract.overdueSince() != null) {
            return Optional.empty();
        }
        return Optional.ofNullable(contract.currentEndDate());
    }

    // ---- Chi tiết -------------------------------------------------------------------------------------------------

    /**
     * Hợp đồng đã ký có khoảng chứa ngày D, bắt đầu muộn nhất: Hiệu lực không có cận trên (quá hạn vẫn áp dụng, FR-029),
     * Kết thúc hoặc Chấm dứt tới ngày kết thúc hiện hành nếu có.
     */
    private Optional<Contract> coveringContract(long residentId, LocalDate date) {
        return store.ofResident(residentId).stream()
                .filter(c -> SIGNED.contains(c.status()))
                .filter(c -> !c.content().startDate().isAfter(date))
                .filter(c -> c.status() == ContractStatus.ACTIVE
                        || c.currentEndDate() == null
                        || !date.isAfter(c.currentEndDate()))
                .max(Comparator.comparing((Contract c) -> c.content().startDate())
                        .thenComparing(Contract::id));
    }

    /** Bỏ {@code recordedUnitPrice} khỏi từng dịch vụ rồi trả riêng thành bảng theo mã dịch vụ (FR-036). */
    private ContractContentView view(ContractVersion version) {
        ObjectNode content = (ObjectNode) version.content().deepCopy();
        Map<Long, BigDecimal> recorded = new LinkedHashMap<>();
        for (JsonNode service : content.get("services")) {
            JsonNode price = ((ObjectNode) service).remove("recordedUnitPrice");
            if (price != null && !price.isNull()) {
                recorded.put(service.get("serviceId").asLong(), price.decimalValue());
            }
        }
        return new ContractContentView(
                version.contractId(),
                version.sequenceNo(),
                version.effectiveFrom(),
                version.addendumId(),
                content,
                recorded);
    }

    /** Loại lưu trú và mức chăm sóc hiện hành là của phiên bản nội dung mới nhất, vì phụ lục không sửa dòng hợp đồng gốc. */
    private ContractView view(Contract c) {
        Optional<ContractVersion> latest = SIGNED.contains(c.status()) ? store.latestVersion(c.id()) : Optional.empty();
        return new ContractView(
                c.id(),
                c.contractNumber(),
                c.residentId(),
                latest.map(v -> ResidenceType.valueOf(
                                v.content().get("residenceType").asString()))
                        .orElse(c.content().residenceType()),
                c.content().startDate(),
                c.content().endDate(),
                c.currentEndDate(),
                latest.map(v -> CareLevel.valueOf(v.content().get("careLevel").asString()))
                        .orElse(c.content().careLevel()),
                c.status(),
                c.overdueSince());
    }
}
