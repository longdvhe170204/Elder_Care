package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.admission.api.dto.UnitPriceView;
import com.eldercare.backend.admission.internal.change.domain.ChangeType;
import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeContent;
import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeContent.ServiceChange;
import com.eldercare.backend.admission.internal.change.persistence.AddendumStore;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.domain.ContractContent;
import com.eldercare.backend.admission.internal.contract.domain.ServiceKind;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.admission.internal.contract.service.DayCareCapacityCheck;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Điều kiện của từng loại thay đổi lưu trú khi duyệt và khi áp dụng (FR-041, FR-045, FR-046, FR-022, FR-040), cùng giá trị
 * trước và ảnh hưởng chi phí do hệ thống điền lúc lập yêu cầu (FR-038). Trả đủ mọi điều kiện vi phạm để liệt kê một lần.
 *
 * <p>Kiểm giường, phân bổ (đổi giường, nội trú từ bán trú) qua {@code BedAllocationCommandApi} và sức chứa khu nghỉ (bán trú) qua
 * {@code DayCareCapacityCheck} của 003. Thiếu giường mới thì loại đổi giường bị chặn bằng điều kiện
 * {@code BED_ALLOCATION_CHECK_UNAVAILABLE} thay vì cho qua mà không kiểm.
 */
@Component
class ResidencyChangeRules {

    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private final ContractStore contracts;
    private final AddendumStore addenda;
    private final UnitPriceApi prices;
    private final ResidentApi residents;
    private final DayCareCapacityCheck capacity;
    private final ObjectProvider<BedAllocationCommandApi> beds;
    private final ObjectProvider<LocationStructureApi> structure;

    ResidencyChangeRules(
            ContractStore contracts,
            AddendumStore addenda,
            UnitPriceApi prices,
            ResidentApi residents,
            DayCareCapacityCheck capacity,
            ObjectProvider<BedAllocationCommandApi> beds,
            ObjectProvider<LocationStructureApi> structure) {
        this.capacity = capacity;
        this.beds = beds;
        this.structure = structure;
        this.contracts = contracts;
        this.addenda = addenda;
        this.prices = prices;
        this.residents = residents;
    }

    /**
     * @param requestId yêu cầu đang xét để loại khỏi kiểm trùng thuộc tính (FR-046); {@code null} khi áp dụng, vì lúc đó
     *     chính yêu cầu giữ thuộc tính
     */
    List<UnmetCondition> check(
            Contract contract, JsonNode base, ResidencyChangeContent change, LocalDate effective, Long requestId) {
        return check(contract, base, change, effective, requestId, null);
    }

    /**
     * Điều kiện áp dụng (FR-045): giường mới kiểm qua phân bổ đã đặt trước lúc duyệt ({@code checkScheduledTransfer}), không
     * kiểm lại như lúc đặt trước (chính phân bổ Tương lai đó sẽ chặn).
     */
    List<UnmetCondition> checkApply(
            Contract contract,
            JsonNode base,
            ResidencyChangeContent change,
            LocalDate effective,
            long applyingRequestId) {
        return check(contract, base, change, effective, null, applyingRequestId);
    }

    private List<UnmetCondition> check(
            Contract contract,
            JsonNode base,
            ResidencyChangeContent change,
            LocalDate effective,
            Long requestId,
            Long applyingRequestId) {
        List<UnmetCondition> unmet = new ArrayList<>();
        if (contract.status() != ContractStatus.ACTIVE) {
            unmet.add(new UnmetCondition("CONTRACT_NOT_ACTIVE", "Hợp đồng gốc không còn Hiệu lực (FR-045)"));
            return unmet;
        }
        if (residents.isFinalStatus(contract.residentId())) {
            unmet.add(new UnmetCondition(
                    "RESIDENT_IN_FINAL_STATUS",
                    "Người cao tuổi đã ở trạng thái cuối, không điều chỉnh hợp đồng (FR-045)"));
        }
        if (effective.isBefore(contract.content().startDate())) {
            unmet.add(new UnmetCondition(
                    "EFFECTIVE_DATE_BEFORE_CONTRACT_START",
                    "Ngày hiệu lực sớm hơn ngày bắt đầu hợp đồng gốc (FR-040, DBR-07)"));
        }
        if (requestId != null) {
            for (ChangeType type : addenda.pendingConflicts(contract.residentId(), change.changeTypes(), requestId)) {
                unmet.add(new UnmetCondition(
                        "DUPLICATE_PENDING_ATTRIBUTE",
                        "Đã có yêu cầu Đã duyệt chờ hiệu lực thay đổi " + type
                                + "; hủy yêu cầu đó hoặc gộp nội dung (FR-046)"));
            }
        }
        ResidenceType baseType = ResidenceType.valueOf(base.get("residenceType").asString());
        ResidenceType resultType = change.residenceType() != null ? change.residenceType() : baseType;
        if (change.has(ChangeType.RESIDENCE_TYPE)) {
            residenceType(unmet, contract.residentId(), change, baseType, effective, applyingRequestId);
        }
        if (change.has(ChangeType.CARE_LEVEL)) {
            careLevel(unmet, base, change, effective);
        }
        if (change.has(ChangeType.SERVICE)) {
            services(unmet, base, change, effective);
        }
        LocalDate endDate = change.endDate();
        if (change.has(ChangeType.DURATION) || (change.has(ChangeType.RESIDENCE_TYPE) && endDate != null)) {
            duration(unmet, contract, endDate, effective);
        }
        if (change.has(ChangeType.BED)) {
            if (resultType == ResidenceType.DAY_CARE) {
                unmet.add(new UnmetCondition("NOT_INPATIENT", "Người bán trú không có giường để đổi (FR-041)"));
            }
            // Sau chuyển gấp (003 FR-030): người đã ở giường mới, không đặt trước nên không kiểm như chuyển theo lịch.
            // Bán trú sang nội trú kèm giường: đã kiểm ở nhánh đổi loại lưu trú (phân bổ đầu tiên).
            if (!change.afterUrgentTransfer() && !change.firstBed(baseType)) {
                unmet.addAll(
                        applyingRequestId != null && change.newBedId() != null
                                ? beds.getObject().checkScheduledTransfer(applyingRequestId)
                                : bedConditions(contract.residentId(), change.newBedId(), effective));
            }
        }
        if (change.has(ChangeType.ARRIVAL_SCHEDULE)) {
            if (resultType != ResidenceType.DAY_CARE) {
                unmet.add(new UnmetCondition("NOT_DAY_CARE", "Chỉ hợp đồng bán trú có lịch đến (BR-M02-16)"));
            } else {
                unmet.addAll(capacityConditions(change, effective));
            }
        }
        return unmet;
    }

    private void residenceType(
            List<UnmetCondition> unmet,
            long residentId,
            ResidencyChangeContent change,
            ResidenceType baseType,
            LocalDate effective,
            Long applyingRequestId) {
        ResidenceType target = change.residenceType();
        if (target == baseType) {
            unmet.add(new UnmetCondition("RESIDENCE_TYPE_UNCHANGED", "Loại lưu trú mới trùng loại hiện hành"));
            return;
        }
        if (target == ResidenceType.SHORT_TERM_RESIDENTIAL && change.endDate() == null) {
            unmet.add(new UnmetCondition("END_DATE_REQUIRED", "Nội trú ngắn ngày cần ngày kết thúc (FR-019)"));
        }
        if (target == ResidenceType.DAY_CARE) {
            if (change.arrivalSchedule().isEmpty()) {
                unmet.add(new UnmetCondition(
                        "ARRIVAL_SCHEDULE_REQUIRED", "Đổi sang bán trú cần kèm lịch đến (FR-041, BR-M02-16)"));
            }
            unmet.addAll(capacityConditions(change, effective));
        } else if (baseType == ResidenceType.DAY_CARE) {
            if (change.newBedId() == null) {
                unmet.add(
                        new UnmetCondition("BED_REQUIRED", "Đổi từ bán trú sang nội trú cần kèm giường mới (FR-041)"));
                return;
            }
            // 003 mục 1.4a: người bán trú chưa có giường; lúc áp dụng kiểm qua phân bổ đã đặt trước lúc duyệt
            unmet.addAll(
                    applyingRequestId != null
                            ? beds.getObject().checkScheduledTransfer(applyingRequestId)
                            : beds.getObject()
                                    .checkFirstAllocationReservation(
                                            residentId,
                                            change.newBedId(),
                                            effective,
                                            firstBedExpectedEnd(change),
                                            residencyType(target)));
        }
    }

    private void careLevel(
            List<UnmetCondition> unmet, JsonNode base, ResidencyChangeContent change, LocalDate effective) {
        if (CareLevel.valueOf(base.get("careLevel").asString()) == change.careLevel()) {
            unmet.add(new UnmetCondition("CARE_LEVEL_UNCHANGED", "Mức chăm sóc mới trùng mức hiện hành"));
            return;
        }
        Optional<ServiceView> care = prices.careServiceOf(change.careLevel());
        if (care.isEmpty()) {
            unmet.add(new UnmetCondition(
                    "CARE_SERVICE_MISSING",
                    "Chưa có dịch vụ chăm sóc Hiệu lực cho mức " + change.careLevel() + " (Q-297)"));
        } else if (prices.unitPriceAt(care.get().id(), effective, false).isEmpty()) {
            unmet.add(new UnmetCondition(
                    "SERVICE_PRICE_MISSING",
                    "Dịch vụ chăm sóc mức " + change.careLevel() + " không có đơn giá hiệu lực tại " + effective));
        }
    }

    private void services(
            List<UnmetCondition> unmet, JsonNode base, ResidencyChangeContent change, LocalDate effective) {
        for (ServiceChange line : change.services()) {
            boolean inContract = hasService(base, line.serviceId());
            Optional<ServiceKind> kind = contracts.serviceKind(line.serviceId());
            if (line.action() == ServiceChange.Action.REMOVE) {
                if (!inContract) {
                    unmet.add(new UnmetCondition(
                            "SERVICE_NOT_IN_CONTRACT", "Dịch vụ " + line.serviceId() + " không có trong hợp đồng"));
                } else if (kind.map(k -> "CARE".equals(k.type())).orElse(false)) {
                    unmet.add(new UnmetCondition(
                            "CARE_SERVICE_NOT_ALLOWED", "Dịch vụ chăm sóc chỉ đổi qua loại đổi mức chăm sóc (Q-297)"));
                }
                continue;
            }
            if (kind.isEmpty()) {
                unmet.add(new UnmetCondition("SERVICE_NOT_FOUND", "Dịch vụ " + line.serviceId() + " không tồn tại"));
            } else if (!kind.get().active()) {
                unmet.add(new UnmetCondition(
                        "SERVICE_NOT_EFFECTIVE", "Dịch vụ " + line.serviceId() + " không Hiệu lực (FR-041)"));
            }
            // Loại dịch vụ không thêm được (gồm dịch vụ chăm sóc, Q-297) kiểm ở
            // ResidencyChangeHandler.serviceTypeConditions
            // (FR-043k), chạy khi lập, sửa, gửi duyệt và duyệt
            if (inContract) {
                unmet.add(new UnmetCondition(
                        "SERVICE_ALREADY_IN_CONTRACT", "Dịch vụ " + line.serviceId() + " đã có trong hợp đồng"));
            }
            if (line.customUnitPrice() == null
                    && priceAt(line.serviceId(), effective).isEmpty()) {
                unmet.add(new UnmetCondition(
                        "SERVICE_PRICE_MISSING",
                        "Dịch vụ " + line.serviceId() + " không có đơn giá hiệu lực tại " + effective + " (FR-041)"));
            }
        }
    }

    private void duration(List<UnmetCondition> unmet, Contract contract, LocalDate endDate, LocalDate effective) {
        if (endDate.isBefore(effective) || endDate.isBefore(contract.content().startDate())) {
            unmet.add(new UnmetCondition(
                    "END_DATE_BEFORE_EFFECTIVE", "Ngày kết thúc mới phải không sớm hơn ngày hiệu lực (FR-041)"));
            return;
        }
        if (contracts.overlapsActive(
                contract.residentId(), contract.id(), contract.content().startDate(), endDate)) {
            unmet.add(new UnmetCondition(
                    "CONTRACT_OVERLAP",
                    "Ngày kết thúc mới làm hợp đồng chồng khoảng với hợp đồng Hiệu lực khác (FR-022, DBR-06)"));
        }
    }

    /** Kết thúc dự kiến của phân bổ đầu tiên: đầu ngày liền sau ngày kết thúc với ngắn ngày, không có với dài ngày. */
    static Instant firstBedExpectedEnd(ResidencyChangeContent change) {
        return change.residenceType() == ResidenceType.SHORT_TERM_RESIDENTIAL && change.endDate() != null
                ? change.endDate().plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant()
                : null;
    }

    static ResidencyType residencyType(ResidenceType type) {
        return type == ResidenceType.SHORT_TERM_RESIDENTIAL ? ResidencyType.SHORT_TERM : ResidencyType.LONG_TERM;
    }

    /** Điều kiện giữ giường theo lịch của 003 (FR-041); thiếu bản cài hoặc thiếu giường thì chưa duyệt được. */
    private List<UnmetCondition> bedConditions(long residentId, Long newBedId, LocalDate effective) {
        if (newBedId == null) {
            return List.of(new UnmetCondition(
                    "BED_ALLOCATION_CHECK_UNAVAILABLE",
                    "Chưa có giường mới để kiểm điều kiện phân bổ; chưa duyệt được (FR-041)"));
        }
        return beds.getObject().checkScheduledReservation(residentId, newBedId, effective);
    }

    private List<UnmetCondition> capacityConditions(ResidencyChangeContent change, LocalDate effective) {
        return capacity.violations(
                change.arrivalSchedule().stream()
                        .map(line -> new ContractContent.ArrivalLine(
                                line.dayOfWeek(), line.date(), line.arrivalTime(), line.departureTime()))
                        .toList(),
                effective,
                null);
    }

    private static boolean hasService(JsonNode base, long serviceId) {
        for (JsonNode line : base.path("services")) {
            if (line.path("serviceId").asLong() == serviceId) {
                return true;
            }
        }
        return false;
    }

    private Optional<BigDecimal> priceAt(long serviceId, LocalDate date) {
        return prices.unitPriceAt(serviceId, date, false).map(UnitPriceView::unitPrice);
    }

    // ---- Giá trị trước và ảnh hưởng chi phí (FR-038) --------------------------------------------------------------

    /** Giá trị hiện hành của các thuộc tính sắp đổi, lấy từ phiên bản nội dung mới nhất. */
    ObjectNode previousValue(JsonNode base, ResidencyChangeContent change) {
        ObjectNode previous = F.objectNode();
        Set<ChangeType> types = change.changeTypes();
        if (types.contains(ChangeType.RESIDENCE_TYPE)) {
            copy(base, previous, "residenceType", "endDate", "bedId", "arrivalSchedule");
        }
        if (types.contains(ChangeType.CARE_LEVEL)) {
            copy(base, previous, "careLevel");
        }
        if (types.contains(ChangeType.SERVICE)) {
            copy(base, previous, "services");
        }
        if (types.contains(ChangeType.DURATION)) {
            copy(base, previous, "endDate");
        }
        if (types.contains(ChangeType.BED)) {
            copy(base, previous, "bedId", "roomTypeId");
        }
        if (types.contains(ChangeType.ARRIVAL_SCHEDULE)) {
            copy(base, previous, "arrivalSchedule");
        }
        if (change.changesDeposit()) {
            copy(base, previous, "noDepositRequired", "depositAmount");
        }
        return previous;
    }

    /**
     * Ảnh hưởng chi phí theo đơn giá (đơn giá trước, sau của từng khoản): dịch vụ thuộc gói không tính riêng nên cả hai
     * bằng 0. Đổi giường, loại lưu trú khác giá cần loại phòng của giường, thuộc 003, nên chưa có khoản ở đây.
     */
    ArrayNode costImpact(JsonNode base, ResidencyChangeContent change, LocalDate effective) {
        ArrayNode items = F.arrayNode();
        if (change.has(ChangeType.CARE_LEVEL)) {
            BigDecimal previous = BigDecimal.ZERO;
            for (JsonNode line : base.path("services")) {
                if (contracts
                        .serviceKind(line.path("serviceId").asLong())
                        .map(k -> "CARE".equals(k.type()))
                        .orElse(false)) {
                    previous = line.path("recordedUnitPrice").decimalValue();
                }
            }
            BigDecimal next = prices.careServiceOf(change.careLevel())
                    .flatMap(care -> priceAt(care.id(), effective))
                    .orElse(BigDecimal.ZERO);
            items.add(item("CARE_SERVICE", previous, next));
        }
        if (change.has(ChangeType.SERVICE)) {
            for (ServiceChange line : change.services()) {
                if (line.action() == ServiceChange.Action.ADD) {
                    // R19: dấu thuộc gói theo hợp đồng hiện hành, bỏ qua dấu người lập gửi
                    BigDecimal price = IsMinorChange.inPackage(base, line.serviceId())
                            ? BigDecimal.ZERO
                            : line.customUnitPrice() != null
                                    ? BigDecimal.valueOf(line.customUnitPrice())
                                    : priceAt(line.serviceId(), effective).orElse(BigDecimal.ZERO);
                    items.add(item("SERVICE:" + line.serviceId(), BigDecimal.ZERO, price));
                } else {
                    BigDecimal price = BigDecimal.ZERO;
                    for (JsonNode current : base.path("services")) {
                        if (current.path("serviceId").asLong() == line.serviceId()
                                && !current.path("inPackage").asBoolean(false)) {
                            price = current.path("recordedUnitPrice").decimalValue();
                        }
                    }
                    items.add(item("SERVICE:" + line.serviceId(), price, BigDecimal.ZERO));
                }
            }
        }
        if (change.newBedId() != null) {
            // Phí lưu trú theo loại phòng của giường mới, như billing tính theo roomTypeId
            BigDecimal previous = base.path("roomTypeId").isNumber()
                    ? residencyFeeAt(base.path("roomTypeId").asLong(), effective)
                    : BigDecimal.ZERO;
            BigDecimal next = structure
                    .getObject()
                    .roomTypeOfBed(change.newBedId())
                    .map(type -> residencyFeeAt(type.id(), effective))
                    .orElse(BigDecimal.ZERO);
            items.add(item("RESIDENCY_FEE", previous, next));
        }
        return items;
    }

    private BigDecimal residencyFeeAt(long roomTypeId, LocalDate date) {
        return prices.residencyFeeServiceOf(roomTypeId)
                .flatMap(fee -> priceAt(fee.id(), date))
                .orElse(BigDecimal.ZERO);
    }

    private static ObjectNode item(String name, BigDecimal previous, BigDecimal next) {
        ObjectNode node = F.objectNode();
        node.put("item", name);
        node.set("previousUnitPrice", number(previous));
        node.set("newUnitPrice", number(next));
        return node;
    }

    private static JsonNode number(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() <= 0 ? F.numberNode(stripped.toBigInteger()) : F.numberNode(stripped);
    }

    private static void copy(JsonNode from, ObjectNode to, String... keys) {
        for (String key : keys) {
            if (from.has(key)) {
                to.set(key, from.get(key));
            }
        }
    }
}
